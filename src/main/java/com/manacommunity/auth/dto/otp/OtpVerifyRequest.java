package com.manacommunity.auth.dto.otp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request to verify a previously emailed one-time code. */
@Data
public class OtpVerifyRequest {
    @NotBlank
    private String email;

    @NotBlank
    private String code;
}
