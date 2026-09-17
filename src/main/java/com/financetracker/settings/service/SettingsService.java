package com.financetracker.settings.service;

import com.financetracker.settings.dto.SettingsResponse;
import com.financetracker.settings.dto.UpdateSettingsRequest;

public interface SettingsService {

    SettingsResponse get(Long userId);

    SettingsResponse update(Long userId, UpdateSettingsRequest request);
}
