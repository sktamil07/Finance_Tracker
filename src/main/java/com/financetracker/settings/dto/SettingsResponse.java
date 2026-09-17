package com.financetracker.settings.dto;

import com.financetracker.user.domain.ThemePreference;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SettingsResponse")
public record SettingsResponse(
        @Schema(description = "The user's display name") String displayName,
        String email,
        @Schema(example = "INR") String currencyCode,
        ThemePreference themePreference
) {
}
