package com.financetracker.user.dto;

import com.financetracker.user.domain.ThemePreference;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "UserResponse")
public record UserResponse(
        Long id,
        String name,
        String email,
        @Schema(example = "INR") String currencyCode,
        ThemePreference themePreference,
        Instant createdAt,
        Instant updatedAt
) {
}
