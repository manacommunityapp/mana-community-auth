package com.manacommunity.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds {@code app.security.registration.*} — the optional anti-abuse controls
 * guarding the public event-registration flow (reCAPTCHA + email OTP).
 *
 * <p>Both features are <strong>disabled by default</strong> so existing
 * authenticated/admin registration paths keep working in dev and in tests; they
 * are switched on in production by setting the env vars.
 */
@Component
@ConfigurationProperties(prefix = "app.security.registration")
@Getter
@Setter
public class RegistrationSecurityProperties {

    private final Recaptcha recaptcha = new Recaptcha();
    private final Otp otp = new Otp();

    @Getter
    @Setter
    public static class Recaptcha {
        /** When false (default), the reCAPTCHA token is not verified. */
        private boolean enabled = false;
        /** Google reCAPTCHA secret key (server-side). Required when enabled. */
        private String secret = "";
        /** Google siteverify endpoint. */
        private String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";
    }

    @Getter
    @Setter
    public static class Otp {
        /** When false (default), email OTP is not required to register. */
        private boolean enabled = false;
        /** Digits in the generated code. */
        private int length = 6;
        /** Minutes a freshly issued code stays valid. */
        private int ttlMinutes = 10;
        /** Max codes that may be sent to one email within a rolling hour. */
        private int maxSendsPerHour = 3;
        /** Max verify attempts allowed per issued code before it is consumed. */
        private int maxVerifyAttempts = 3;
        /** How long (minutes) a verified email stays "unlocked" for registration. */
        private int verifiedWindowMinutes = 30;
    }
}
