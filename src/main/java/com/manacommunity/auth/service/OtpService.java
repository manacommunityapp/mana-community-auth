package com.manacommunity.auth.service;

import com.manacommunity.auth.config.RegistrationSecurityProperties;
import com.manacommunity.auth.dto.otp.OtpResponse;
import com.manacommunity.common.email.EmailMessage;
import com.manacommunity.common.email.EmailService;
import com.manacommunity.common.email.EmailSupport;
import com.manacommunity.common.enums.EmailTemplate;
import com.manacommunity.common.util.EmailTemplateRenderer;
import com.manacommunity.common.exception.ManaCommunityException;
import com.manacommunity.auth.model.EmailOtp;
import com.manacommunity.auth.repository.EmailOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Email one-time-passcode flow used to verify a registrant's email before they
 * may submit an event registration.
 *
 * <ul>
 *   <li>{@link #send} — issues a 6-digit code (hashed at rest), emails it, and
 *       rate-limits to N codes per email per rolling hour.</li>
 *   <li>{@link #verify} — checks a submitted code against the latest active one,
 *       capping attempts; on success the email is marked verified for a window.</li>
 *   <li>{@link #assertEmailVerified} — the registration gate; a no-op when the
 *       feature is disabled.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RegistrationSecurityProperties props;
    private final EmailOtpRepository otpRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailTemplateRenderer renderer;
    private final EmailSupport support;

    @Transactional
    public OtpResponse send(String rawEmail, String name) {
        RegistrationSecurityProperties.Otp cfg = props.getOtp();
        String email = normalize(rawEmail);

        // Rate limit: cap codes issued to this email within the rolling hour.
        long recent = otpRepo.countByEmailAndCreatedAtAfter(email, LocalDateTime.now().minusHours(1));
        if (recent >= cfg.getMaxSendsPerHour()) {
            throw new ManaCommunityException(
                    "Too many codes requested. Please try again later.",
                    HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED");
        }

        String code = generateCode(cfg.getLength());
        EmailOtp otp = EmailOtp.builder()
                .email(email)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(LocalDateTime.now().plusMinutes(cfg.getTtlMinutes()))
                .attempts(0)
                .consumed(false)
                .verified(false)
                .build();
        otpRepo.save(otp);

        sendCodeEmail(email, name, code, cfg.getTtlMinutes());
        return OtpResponse.sent();
    }

    @Transactional
    public OtpResponse verify(String rawEmail, String code) {
        RegistrationSecurityProperties.Otp cfg = props.getOtp();
        String email = normalize(rawEmail);

        EmailOtp otp = otpRepo.findFirstByEmailAndConsumedFalseOrderByCreatedAtDesc(email).orElse(null);
        if (otp == null) {
            return OtpResponse.failure("No active code. Please request a new one.");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setConsumed(true);
            otpRepo.save(otp);
            return OtpResponse.failure("Your code has expired. Please request a new one.");
        }
        if (otp.getAttempts() >= cfg.getMaxVerifyAttempts()) {
            otp.setConsumed(true);
            otpRepo.save(otp);
            return OtpResponse.failure("Too many incorrect attempts. Please request a new code.");
        }

        if (passwordEncoder.matches(code == null ? "" : code.trim(), otp.getCodeHash())) {
            otp.setVerified(true);
            otp.setVerifiedAt(LocalDateTime.now());
            otp.setConsumed(true);
            otpRepo.save(otp);
            return OtpResponse.ok();
        }

        otp.setAttempts(otp.getAttempts() + 1);
        boolean exhausted = otp.getAttempts() >= cfg.getMaxVerifyAttempts();
        if (exhausted) {
            otp.setConsumed(true);
        }
        otpRepo.save(otp);
        return OtpResponse.failure(exhausted
                ? "Incorrect code. You've run out of attempts — please request a new code."
                : "Incorrect code. Please try again.");
    }

    /**
     * Registration gate: when OTP is enabled, require that {@code email} was
     * verified within the configured window. No-op when the feature is off.
     */
    public void assertEmailVerified(String rawEmail) {
        RegistrationSecurityProperties.Otp cfg = props.getOtp();
        if (!cfg.isEnabled()) {
            return;
        }
        String email = normalize(rawEmail);
        LocalDateTime since = LocalDateTime.now().minusMinutes(cfg.getVerifiedWindowMinutes());
        if (!otpRepo.existsByEmailAndVerifiedTrueAndVerifiedAtAfter(email, since)) {
            throw new ManaCommunityException(
                    "Please verify your email before registering.",
                    HttpStatus.BAD_REQUEST, "EMAIL_NOT_VERIFIED");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void sendCodeEmail(String email, String name, String code, int ttlMinutes) {
        try {
            Map<String, Object> vars = support.baseVars(name);
            vars.put("otpCode", code);
            vars.put("expiryMinutes", ttlMinutes);
            String html = renderer.render(EmailTemplate.EMAIL_OTP, vars);
            emailService.send(new EmailMessage(
                    email, name, EmailTemplate.EMAIL_OTP.defaultSubject(), html));
        } catch (Exception e) {
            // Never fail the request because the email layer hiccuped — it logs internally.
            log.error("Failed to send OTP email to {}: {}", maskEmail(email), e.getMessage());
        }
    }

    private static String generateCode(int length) {
        int digits = Math.max(4, Math.min(length, 9));
        int bound = (int) Math.pow(10, digits);
        int min = (int) Math.pow(10, digits - 1);
        int value = min + RANDOM.nextInt(bound - min);
        return String.valueOf(value);
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
