package com.bharath.financetracker.settings.service;

import com.bharath.financetracker.settings.dto.SettingsResponse;
import com.bharath.financetracker.settings.dto.UpdateSettingsRequest;

public interface SettingsService {

    SettingsResponse get(Long userId);

    SettingsResponse update(Long userId, UpdateSettingsRequest request);
}
