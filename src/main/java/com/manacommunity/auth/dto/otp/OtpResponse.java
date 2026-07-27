package com.manacommunity.auth.dto.otp;

/**
 * Result of an OTP send/verify call.
 *
 * @param success   whether the operation succeeded
 * @param verified  true once the email is verified (verify endpoint only)
 * @param message   human-readable status for the UI
 */
public record OtpResponse(boolean success, boolean verified, String message) {

    public static OtpResponse sent() {
        return new OtpResponse(true, false, "A verification code has been sent to your email.");
    }

    public static OtpResponse ok() {
        return new OtpResponse(true, true, "Email verified.");
    }

    public static OtpResponse failure(String message) {
        return new OtpResponse(false, false, message);
    }
}
