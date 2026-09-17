package com.financetracker.user.service;

import com.financetracker.user.domain.User;
import com.financetracker.user.dto.UserResponse;

public interface UserService {

    UserResponse getCurrentUser(Long userId);

    /**
     * Service-layer only: returns the entity for other services to attach as an FK owner.
     * Never call this from a controller.
     */
    User requireUser(Long userId);
}
