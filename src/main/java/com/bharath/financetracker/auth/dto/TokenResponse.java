package com.bharath.financetracker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TokenResponse")
public record TokenResponse(

        @Schema(description = "Short-lived bearer token (~15 min)") String accessToken,

        @Schema(description = "Long-lived token (~7 days), exchanged at /auth/refresh") String refreshToken,

        @Schema(example = "Bearer") String tokenType,

        @Schema(description = "Access token lifetime in seconds", example = "900") long expiresIn
) {

    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
