package com.bharath.financetracker.mappers;

import com.bharath.financetracker.settings.dto.SettingsResponse;
import com.bharath.financetracker.user.domain.User;
import com.bharath.financetracker.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "displayName", source = "name")
    SettingsResponse toSettingsResponse(User user);
}
