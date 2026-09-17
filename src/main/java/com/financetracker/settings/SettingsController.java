package com.financetracker.settings;

import com.financetracker.security.AuthPrincipal;
import com.financetracker.settings.dto.SettingsResponse;
import com.financetracker.settings.dto.UpdateSettingsRequest;
import com.financetracker.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Settings", description = "Theme, currency, and display name")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @Operation(summary = "Read the authenticated user's settings")
    @GetMapping
    public SettingsResponse get(@AuthenticationPrincipal AuthPrincipal principal) {
        return settingsService.get(principal.id());
    }

    @Operation(summary = "Update theme, currency, and display name")
    @PutMapping
    public SettingsResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                                   @Valid @RequestBody UpdateSettingsRequest request) {
        return settingsService.update(principal.id(), request);
    }
}
