package com.manacommunity.auth.user.controller;

import com.manacommunity.auth.user.dto.AuthResponse;
import com.manacommunity.common.security.JwtTokenProvider;
import com.manacommunity.common.security.OidcService;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.repository.AppUserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth/oidc")
@RequiredArgsConstructor
public class OidcController {

    private final OidcService oidcService;
    private final AppUserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Data
    public static class OidcLoginRequest {
        private String idToken;
    }

    /**
     * Authenticates or auto-provisions a user using OpenID Connect (Google/Apple/SSO).
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> oidcLogin(@RequestBody OidcLoginRequest request) {
        OidcService.OidcUserProfile profile = oidcService.validateAndExtractProfile(request.getIdToken());
        log.info("OIDC token validated for email: [{}], iss: [{}]", profile.getEmail(), profile.getIssuer());

        Optional<AppUser> existing = userRepository.findByEmail(profile.getEmail());
        AppUser user;
        if (existing.isPresent()) {
            user = existing.get();
        } else {
            // Auto-provision verified user
            user = AppUser.builder()
                    .email(profile.getEmail())
                    .fullName(profile.getName() != null ? profile.getName() : profile.getEmail())
                    .phone("OIDC_" + (profile.getSubject() != null ? profile.getSubject().substring(0, Math.min(8, profile.getSubject().length())) : "SSO"))
                    .passwordHash("OIDC_MANAGED_IDENTITY")
                    .dateOfBirth(LocalDate.of(2000, 1, 1))
                    .gender("OTHER")
                    .role("MEMBER")
                    .kycStatus("VERIFIED")
                    .isActive(true)
                    .profilePicUrl(profile.getPicture())
                    .build();
            user = userRepository.save(user);
            log.info("Auto-provisioned new OIDC user: [{}]", user.getEmail());
        }

        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        AuthResponse resp = new AuthResponse(
                String.valueOf(user.getId()),
                "OIDC authentication successful",
                token,
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCommunity() != null ? user.getCommunity().getId() : null,
                user.getDateOfBirth()
        );
        resp.setRefreshToken(refreshToken);

        return ResponseEntity.ok(resp);
    }
}
