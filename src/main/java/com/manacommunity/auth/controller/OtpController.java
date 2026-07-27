package com.manacommunity.auth.controller;

import com.manacommunity.auth.dto.otp.OtpResponse;
import com.manacommunity.auth.dto.otp.OtpSendRequest;
import com.manacommunity.auth.dto.otp.OtpVerifyRequest;
import com.manacommunity.auth.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public email-OTP endpoints backing the secure registration flow:
 * <ul>
 *   <li>{@code POST /api/otp/send}   — email a one-time code</li>
 *   <li>{@code POST /api/otp/verify} — validate a submitted code</li>
 * </ul>
 * Permitted unauthenticated in {@code SecurityConfig}; abuse is bounded by the
 * service's per-email rate limit and attempt cap.
 */
@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<OtpResponse> send(@Valid @RequestBody OtpSendRequest req) {
        return ResponseEntity.ok(otpService.send(req.getEmail(), req.getName()));
    }

    @PostMapping("/verify")
    public ResponseEntity<OtpResponse> verify(@Valid @RequestBody OtpVerifyRequest req) {
        OtpResponse resp = otpService.verify(req.getEmail(), req.getCode());
        return ResponseEntity.ok(resp);
    }
}
