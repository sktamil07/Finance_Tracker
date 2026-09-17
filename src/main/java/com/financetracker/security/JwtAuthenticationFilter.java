package com.financetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates {@code Authorization: Bearer <access token>}. A bad token never throws out of the
 * chain; it leaves the context empty and records a reason for {@link RestAuthenticationEntryPoint}.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "com.financetracker.authError";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                Claims claims = jwtService.parse(token);
                if (jwtService.typeOf(claims) != TokenType.ACCESS) {
                    request.setAttribute(AUTH_ERROR_ATTRIBUTE,
                            "A refresh token cannot be used to access resources");
                } else {
                    authenticate(request, jwtService.principalOf(claims));
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Invalid or expired access token");
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, AuthPrincipal principal) {
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
