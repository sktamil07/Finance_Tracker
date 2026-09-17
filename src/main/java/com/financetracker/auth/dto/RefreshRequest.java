package com.financetracker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshRequest")
public record RefreshRequest(

        @NotBlank @Schema(description = "The refreshToken returned by /auth/login") String refreshToken
) {
}
