package com.bharath.financetracker.settings.dto;

import com.bharath.financetracker.user.domain.ThemePreference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateSettingsRequest")
public record UpdateSettingsRequest(

        @NotBlank @Size(max = 100) @Schema(example = "Bharath") String displayName,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be a 3-letter ISO 4217 code")
        @Schema(example = "INR")
        String currencyCode,

        @NotNull @Schema(example = "DARK") ThemePreference themePreference
) {
}
