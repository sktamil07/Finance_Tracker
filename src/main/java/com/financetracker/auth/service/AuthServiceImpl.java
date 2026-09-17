package com.financetracker.auth.service;

import com.financetracker.auth.dto.LoginRequest;
import com.financetracker.auth.dto.RefreshRequest;
import com.financetracker.auth.dto.RegisterRequest;
import com.financetracker.auth.dto.TokenResponse;
import com.financetracker.category.service.CategoryService;
import com.financetracker.common.exception.ConflictException;
import com.financetracker.security.AuthPrincipal;
import com.financetracker.security.JwtService;
import com.financetracker.security.TokenType;
import com.financetracker.user.domain.User;
import com.financetracker.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with that email already exists");
        }

        User user = userRepository.save(User.builder()
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build());

        categoryService.seedDefaults(user);

        return issueTokens(user.getId(), user.getEmail());
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueTokens(user.getId(), user.getEmail());
    }

    @Override
    public TokenResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        if (jwtService.typeOf(claims) != TokenType.REFRESH) {
            throw new BadCredentialsException("Expected a refresh token");
        }

        AuthPrincipal principal = jwtService.principalOf(claims);

        // The user may have been deleted since the refresh token was minted.
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        return issueTokens(user.getId(), user.getEmail());
    }

    private TokenResponse issueTokens(Long userId, String email) {
        return TokenResponse.of(
                jwtService.generateAccessToken(userId, email),
                jwtService.generateRefreshToken(userId, email),
                jwtService.accessTokenExpiresInSeconds());
    }
}
