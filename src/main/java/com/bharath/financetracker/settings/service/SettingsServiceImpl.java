package com.bharath.financetracker.settings.service;

import com.bharath.financetracker.mappers.UserMapper;
import com.bharath.financetracker.settings.dto.SettingsResponse;
import com.bharath.financetracker.settings.dto.UpdateSettingsRequest;
import com.bharath.financetracker.user.domain.User;
import com.bharath.financetracker.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsServiceImpl implements SettingsService {

    private final UserService userService;
    private final UserMapper userMapper;

    @Override
    public SettingsResponse get(Long userId) {
        return userMapper.toSettingsResponse(userService.requireUser(userId));
    }

    @Override
    @Transactional
    public SettingsResponse update(Long userId, UpdateSettingsRequest request) {
        User user = userService.requireUser(userId);
        user.setName(request.displayName());
        user.setCurrencyCode(request.currencyCode());
        user.setThemePreference(request.themePreference());
        // Dirty checking flushes on commit; no explicit save needed.
        return userMapper.toSettingsResponse(user);
    }
}
