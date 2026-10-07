package com.manacommunity.auth.unit.service;

import com.manacommunity.auth.user.dto.LoginRequest;
import com.manacommunity.auth.user.dto.RegisterRequest;
import com.manacommunity.common.exception.DuplicateResourceException;
import com.manacommunity.auth.exception.InvalidInviteCodeException;
import com.manacommunity.common.exception.ManaCommunityException;
import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.model.Community;
import com.manacommunity.common.model.Role;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.auth.repository.CommunityRepository;
import com.manacommunity.auth.repository.RoleRepository;
import com.manacommunity.auth.user.dto.AuthResponse;
import com.manacommunity.auth.user.service.impl.AuthServiceImpl;
import com.manacommunity.auth.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock AppUserRepository userRepository;
    @Mock CommunityRepository communityRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock com.manacommunity.common.security.JwtTokenProvider jwtTokenProvider;
    @Mock com.manacommunity.common.security.AuditLogService auditLog;
    @Mock com.manacommunity.common.security.AuditService auditService;
    @Mock com.manacommunity.auth.user.security.SessionService sessionService;
    @Mock com.manacommunity.auth.service.FieldEncryptionService fieldEncryptionService;
    @Mock com.manacommunity.auth.service.CommunityModuleService communityModuleService;

    @InjectMocks AuthServiceImpl authService;

    // ── register ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerUser")
    class Register {

        @Test
        @DisplayName("happy path returns AuthResponse with correct email")
        void happyPath() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            Community community = TestDataBuilder.community(1L, "INVITE123");
            Role role = TestDataBuilder.memberRole();

            when(communityRepository.findByInviteCode("INVITE123")).thenReturn(Optional.of(community));
            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(userRepository.existsByPhone(req.getPhone())).thenReturn(false);
            when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed");
            when(roleRepository.findByNameIgnoreCaseAndCommunityId("MEMBER", 1L)).thenReturn(Optional.of(role));
            when(jwtTokenProvider.generateToken(any(AppUser.class))).thenReturn("mock-token-xyz");
            when(jwtTokenProvider.generateRefreshToken(any(AppUser.class))).thenReturn("mock-refresh-xyz");
            when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
                AppUser u = inv.getArgument(0);
                u.setId(99L);
                return u;
            });

            AuthResponse response = authService.registerUser(req);

            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(req.getEmail());
            assertThat(response.getToken()).startsWith("mock-token-");
            verify(userRepository).save(any(AppUser.class));
        }

        @Test
        @DisplayName("invalid invite code throws InvalidInviteCodeException")
        void invalidInviteCode() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(communityRepository.findByInviteCode(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerUser(req))
                    .isInstanceOf(InvalidInviteCodeException.class);
        }

        @Test
        @DisplayName("duplicate email throws DuplicateResourceException")
        void duplicateEmail() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(communityRepository.findByInviteCode(anyString()))
                    .thenReturn(Optional.of(TestDataBuilder.community()));
            when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.registerUser(req))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("duplicate phone throws DuplicateResourceException")
        void duplicatePhone() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(communityRepository.findByInviteCode(anyString()))
                    .thenReturn(Optional.of(TestDataBuilder.community()));
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(req.getPhone())).thenReturn(true);

            assertThatThrownBy(() -> authService.registerUser(req))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("Aadhaar number is masked in saved user")
        void aadhaarMasked() {
            RegisterRequest req = TestDataBuilder.registerRequest();
            req.setAadharNumber("987654321099");

            when(communityRepository.findByInviteCode(anyString()))
                    .thenReturn(Optional.of(TestDataBuilder.community()));
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(roleRepository.findByNameIgnoreCaseAndCommunityId(anyString(), anyLong()))
                    .thenReturn(Optional.of(TestDataBuilder.memberRole()));
            when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
                AppUser u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            authService.registerUser(req);

            verify(userRepository).save(argThat(u ->
                    u.getGovtIdNumber() != null && u.getGovtIdNumber().endsWith("1099")
            ));
        }
    }

    // ── login ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("loginUser")
    class Login {

        @Test
        @DisplayName("correct credentials return AuthResponse")
        void correctCredentials() {
            AppUser user = TestDataBuilder.adminUser();
            user.setPasswordHash("hashed");

            LoginRequest req = TestDataBuilder.loginRequest(user.getEmail(), "password123");

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

            AuthResponse response = authService.loginUser(req);

            assertThat(response.getEmail()).isEqualTo(user.getEmail());
            assertThat(response.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("wrong password throws ManaCommunityException with 401")
        void wrongPassword() {
            AppUser user = TestDataBuilder.adminUser();
            user.setPasswordHash("hashed");

            LoginRequest req = TestDataBuilder.loginRequest(user.getEmail(), "wrong");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.loginUser(req))
                    .isInstanceOf(ManaCommunityException.class);
        }

        @Test
        @DisplayName("unknown email throws generic invalid credentials ManaCommunityException")
        void unknownEmail() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.loginUser(TestDataBuilder.loginRequest("x@x.com", "p")))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Invalid email or password");
        }
    }
}
