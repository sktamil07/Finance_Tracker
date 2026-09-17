package com.financetracker.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Bound from {@code app.jwt.*}. The secret has no default — the app must not boot without one.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @NotBlank
        @Size(min = 32, message = "JWT_SECRET must be at least 32 bytes for HS256")
        String secret,

        @NotBlank String issuer,

        @NotNull Duration accessTokenTtl,

        @NotNull Duration refreshTokenTtl
) {
}
