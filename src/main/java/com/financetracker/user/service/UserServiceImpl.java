package com.financetracker.user.service;

import com.financetracker.common.exception.ResourceNotFoundException;
import com.financetracker.mappers.UserMapper;
import com.financetracker.user.domain.User;
import com.financetracker.user.dto.UserResponse;
import com.financetracker.user.repository.UserRepository;
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
