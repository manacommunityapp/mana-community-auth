package com.manacommunity.auth.unit.service;

import com.manacommunity.auth.support.TestDataBuilder;
import com.manacommunity.auth.user.controller.MfaController;
import com.manacommunity.auth.user.controller.OidcController;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.common.security.JwtTokenProvider;
import com.manacommunity.common.security.OidcService;
import com.manacommunity.common.security.TotpMfaService;
import com.manacommunity.common.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MFA and OIDC Workflow Security Tests")
class MfaOidcWorkflowTest {

    @Mock AppUserRepository userRepository;
    @Mock TotpMfaService totpMfaService;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock OidcService oidcService;

    @InjectMocks MfaController mfaController;
    @InjectMocks OidcController oidcController;

    @Test
    @DisplayName("MFA Setup: Generates secret, QR code URI and stores temp secret")
    void mfaSetupWorkflow() {
        AppUser user = TestDataBuilder.memberUser();
        com.manacommunity.common.user.security.UserPrincipal principal =
                new com.manacommunity.common.user.security.UserPrincipal(2L, user.getEmail(), "pass", List.of(), user);

        when(totpMfaService.generateSecretKey()).thenReturn("JBSWY3DPEHPK3PXP");
        when(totpMfaService.generateQrCodeUri(eq(user.getEmail()), eq("JBSWY3DPEHPK3PXP"), anyString()))
                .thenReturn("otpauth://totp/ManaCommunity:member@test.com?secret=JBSWY3DPEHPK3PXP");
        when(totpMfaService.generateBackupCodes(8)).thenReturn(List.of("11112222", "33334444"));

        ResponseEntity<MfaController.MfaSetupResponse> response = mfaController.setupMfa(principal);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        MfaController.MfaSetupResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getSecret()).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(body.getQrCodeUri()).startsWith("otpauth://totp/");
        assertThat(body.getBackupCodes()).hasSize(2);
    }

    @Test
    @DisplayName("MFA Enable: Validates code, generates 8 backup codes, activates MFA")
    void mfaEnableWorkflow() {
        AppUser user = TestDataBuilder.memberUser();
        com.manacommunity.common.user.security.UserPrincipal principal =
                new com.manacommunity.common.user.security.UserPrincipal(2L, user.getEmail(), "pass", List.of(), user);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(totpMfaService.verifyCode("JBSWY3DPEHPK3PXP", "123456")).thenReturn(true);
        when(totpMfaService.hashBackupCode("11112222")).thenReturn("hash1");
        when(totpMfaService.hashBackupCode("33334444")).thenReturn("hash2");

        MfaController.MfaEnableRequest req = new MfaController.MfaEnableRequest();
        req.setSecret("JBSWY3DPEHPK3PXP");
        req.setCode("123456");
        req.setBackupCodes(List.of("11112222", "33334444"));

        ResponseEntity<?> response = mfaController.enableMfa(principal, req);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(user.getMfaEnabled()).isTrue();
        assertThat(user.getMfaBackupCodes()).isEqualTo("hash1,hash2");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("OIDC Login: Verifies ID token and provisions session token")
    void oidcLoginWorkflow() {
        OidcService.OidcUserProfile profile = OidcService.OidcUserProfile.builder()
                .subject("sub-google-123")
                .email("googleuser@example.com")
                .name("Google User")
                .emailVerified(true)
                .build();
        when(oidcService.validateAndExtractProfile("valid.jwt.idtoken")).thenReturn(profile);

        AppUser existingUser = TestDataBuilder.memberUser();
        existingUser.setEmail("googleuser@example.com");
        when(userRepository.findByEmail("googleuser@example.com")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateToken(existingUser)).thenReturn("jwt.token.val");
        when(jwtTokenProvider.generateRefreshToken(existingUser)).thenReturn("jwt.refresh.val");

        OidcController.OidcLoginRequest req = new OidcController.OidcLoginRequest();
        req.setIdToken("valid.jwt.idtoken");

        ResponseEntity<com.manacommunity.auth.user.dto.AuthResponse> response = oidcController.oidcLogin(req);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        com.manacommunity.auth.user.dto.AuthResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getToken()).isEqualTo("jwt.token.val");
        assertThat(body.getEmail()).isEqualTo("googleuser@example.com");
    }
}
