package com.bharath.financetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_EMAIL = "email";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String email) {
        return generate(userId, email, TokenType.ACCESS, properties.accessTokenTtl());
    }

    public String generateRefreshToken(Long userId, String email) {
        return generate(userId, email, TokenType.REFRESH, properties.refreshTokenTtl());
    }

    /**
     * @throws JwtException if the signature, issuer, or expiry does not check out.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public TokenType typeOf(Claims claims) {
        String raw = claims.get(CLAIM_TYPE, String.class);
        try {
            return TokenType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JwtException("Token is missing a valid '%s' claim".formatted(CLAIM_TYPE));
        }
    }

    public AuthPrincipal principalOf(Claims claims) {
        return new AuthPrincipal(Long.valueOf(claims.getSubject()), claims.get(CLAIM_EMAIL, String.class));
    }

    public long accessTokenExpiresInSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    private String generate(Long userId, String email, TokenType type, Duration ttl) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_TYPE, type.name())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }
}
