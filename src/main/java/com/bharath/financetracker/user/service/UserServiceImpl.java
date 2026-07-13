package com.bharath.financetracker.user.service;

import com.bharath.financetracker.common.exception.ResourceNotFoundException;
import com.bharath.financetracker.mappers.UserMapper;
import com.bharath.financetracker.user.domain.User;
import com.bharath.financetracker.user.dto.UserResponse;
import com.bharath.financetracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getCurrentUser(Long userId) {
        return userMapper.toResponse(requireUser(userId));
    }

    @Override
    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}
