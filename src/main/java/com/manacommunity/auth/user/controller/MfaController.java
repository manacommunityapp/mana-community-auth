package com.manacommunity.auth.user.controller;

import com.manacommunity.auth.user.dto.AuthResponse;
import com.manacommunity.common.security.JwtTokenProvider;
import com.manacommunity.common.security.TotpMfaService;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.common.user.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final TotpMfaService mfaService;
    private final AppUserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Data
    public static class MfaSetupResponse {
        private String secret;
        private String qrCodeUri;
        private List<String> backupCodes;
    }

    @Data
    public static class MfaEnableRequest {
        private String secret;
        private String code;
        private List<String> backupCodes;
    }

    @Data
    public static class MfaVerifyRequest {
        private String email;
        private String code;
    }

    @Data
    public static class MfaRecoveryRequest {
        private String email;
        private String backupCode;
    }

    /**
     * Stage 1: Setup MFA — Generate secret, QR code URI, and backup codes.
     */
    @PostMapping("/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal UserPrincipal principal) {
        String secret = mfaService.generateSecretKey();
        String qrUri = mfaService.generateQrCodeUri(principal.getUsername(), secret, "ManaCommunity");
        List<String> backupCodes = mfaService.generateBackupCodes(8);

        MfaSetupResponse resp = new MfaSetupResponse();
        resp.setSecret(secret);
        resp.setQrCodeUri(qrUri);
        resp.setBackupCodes(backupCodes);
        return ResponseEntity.ok(resp);
    }

    /**
     * Stage 2: Enable MFA — Verify first 6-digit TOTP code and persist secret & hashed backup codes.
     */
    @PostMapping("/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> enableMfa(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody MfaEnableRequest req) {

        if (!mfaService.verifyCode(req.getSecret(), req.getCode())) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_CODE", "message", "Invalid verification code"));
        }

        AppUser user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setMfaEnabled(true);
        user.setMfaSecret(req.getSecret());

        if (req.getBackupCodes() != null && !req.getBackupCodes().isEmpty()) {
            List<String> hashedCodes = req.getBackupCodes().stream()
                    .map(mfaService::hashBackupCode)
                    .toList();
            user.setMfaBackupCodes(String.join(",", hashedCodes));
        }

        userRepository.save(user);
        log.info("MFA enabled for user [{}]", user.getEmail());
        return ResponseEntity.ok(Map.of("message", "MFA successfully enabled"));
    }

    /**
     * Stage 3: Verify MFA Code during Login.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest req) {
        AppUser user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!Boolean.TRUE.equals(user.getMfaEnabled()) || user.getMfaSecret() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "MFA_NOT_ENABLED"));
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), req.getCode())) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_MFA_CODE", "message", "Invalid 6-digit MFA code"));
        }

        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        AuthResponse resp = new AuthResponse(
                String.valueOf(user.getId()),
                "MFA verification successful",
                token,
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCommunity() != null ? user.getCommunity().getId() : null,
                user.getDateOfBirth()
        );
        resp.setRefreshToken(refreshToken);
        resp.setMfaRequired(false);

        return ResponseEntity.ok(resp);
    }

    /**
     * Stage 4: Recover account using single-use backup code.
     */
    @PostMapping("/recovery")
    public ResponseEntity<?> recoveryLogin(@RequestBody MfaRecoveryRequest req) {
        AppUser user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!Boolean.TRUE.equals(user.getMfaEnabled()) || user.getMfaBackupCodes() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "NO_BACKUP_CODES"));
        }

        String inputHash = mfaService.hashBackupCode(req.getBackupCode());
        List<String> storedHashes = new ArrayList<>(Arrays.asList(user.getMfaBackupCodes().split(",")));

        if (!storedHashes.contains(inputHash)) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_BACKUP_CODE"));
        }

        // Burn the single-use backup code
        storedHashes.remove(inputHash);
        user.setMfaBackupCodes(String.join(",", storedHashes));
        userRepository.save(user);

        log.warn("Emergency backup code redeemed for user [{}]", user.getEmail());

        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        AuthResponse resp = new AuthResponse(
                String.valueOf(user.getId()),
                "Emergency recovery successful",
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

    /**
     * Disable MFA for user.
     */
    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> disableMfa(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaBackupCodes(null);
        userRepository.save(user);

        log.info("MFA disabled for user [{}]", user.getEmail());
        return ResponseEntity.ok(Map.of("message", "MFA disabled successfully"));
    }
}
