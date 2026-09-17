package com.financetracker.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Bound from {@code app.cors.*}; the frontend origin is configurable per environment.
 */
@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(

        @NotEmpty List<String> allowedOrigins,

        @NotEmpty List<String> allowedMethods,

        @NotEmpty List<String> allowedHeaders,

        boolean allowCredentials,

        long maxAge
) {
}
