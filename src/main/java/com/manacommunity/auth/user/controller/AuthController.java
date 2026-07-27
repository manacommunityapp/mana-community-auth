package com.manacommunity.auth.user.controller;

import com.manacommunity.auth.user.dto.AuthResponse;

import com.manacommunity.auth.user.dto.LoginRequest;
import com.manacommunity.auth.user.dto.RefreshTokenRequest;
import com.manacommunity.auth.user.dto.RegisterRequest;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.auth.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Auth endpoints — exceptions bubble up to GlobalExceptionHandler
 * automatically.
 * No try/catch needed here; the handler returns the correct HTTP status +
 * ErrorResponse JSON.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) throws Exception {
        AuthResponse response = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) throws Exception {
        return ResponseEntity.ok(authService.loginUser(request));
    }

    /**
     * Exchanges a valid refresh token for a fresh access + refresh token pair.
     * Call this when the access token has (or is about to) expire.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    /**
     * Logout. Tokens are stateless so the server cannot revoke them — the client must
     * discard both tokens. This endpoint records the logout for the audit trail.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(
                principal != null ? principal.getId() : null,
                principal != null ? principal.getUsername() : null);
        return ResponseEntity.ok("Logged out.");
    }

    // KYC endpoint removed — the implementation was never wired up but the
    // endpoint accepted (and discarded) sensitive government-ID data.
}
