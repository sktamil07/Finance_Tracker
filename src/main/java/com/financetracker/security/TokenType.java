package com.financetracker.security;

/**
 * Stamped into the {@code typ} claim so a refresh token can never be replayed as an access token.
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
