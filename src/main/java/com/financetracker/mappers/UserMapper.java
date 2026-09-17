package com.financetracker.mappers;

import com.financetracker.settings.dto.SettingsResponse;
import com.financetracker.user.domain.User;
import com.financetracker.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "displayName", source = "name")
    SettingsResponse toSettingsResponse(User user);
}
