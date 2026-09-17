package com.financetracker.security;

/**
 * What lands in {@code SecurityContextHolder} after a valid access token. Controllers read the
 * user id from here; nothing else is trusted from the request.
 */
public record AuthPrincipal(Long id, String email) {
}
