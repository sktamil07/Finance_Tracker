package com.bharath.financetracker.auth.service;

import com.bharath.financetracker.auth.dto.LoginRequest;
import com.bharath.financetracker.auth.dto.RefreshRequest;
import com.bharath.financetracker.auth.dto.RegisterRequest;
import com.bharath.financetracker.auth.dto.TokenResponse;

public interface AuthService {

    /** Creates the user, seeds their default categories, and returns a fresh token pair. */
    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    /** Rotates the pair: a used refresh token yields a new access token and a new refresh token. */
    TokenResponse refresh(RefreshRequest request);
}
