package com.manacommunity.auth.dto.otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request to email a one-time verification code. */
@Data
public class OtpSendRequest {
    @NotBlank
    @Email
    private String email;

    /** Optional recipient display name for a friendlier email greeting. */
    private String name;
}
