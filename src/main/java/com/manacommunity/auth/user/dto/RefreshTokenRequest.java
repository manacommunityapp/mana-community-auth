package com.manacommunity.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Body for {@code POST /api/auth/refresh} — exchanges a valid refresh token for a new token pair. */
@Data
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
}
