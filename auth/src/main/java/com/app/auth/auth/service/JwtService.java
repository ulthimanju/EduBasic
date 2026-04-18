package com.app.auth.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Stateless JWT utility — all six methods are atomic and side-effect-free.
 *
 * <p>Uses JJWT 0.12 API. Algorithm: HS256 (HMAC-SHA256).
 * Secret is derived from the {@code app.jwt.secret} environment variable.</p>
 *
 * <p>Atomic method rule: every method parses the token independently.
 * No shared parser state. No field caching of parsed claims.</p>
 */
@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-seconds}")
    private long expirationSeconds;

    // ── Generation ────────────────────────────────────────────────────────────

    /**
     * Generate a signed JWT.
     *
     * @param userId  internal user UUID (becomes JWT subject)
     * @param email   user email (stored as claim)
     * @param jwtId   unique token ID = UUID (becomes "jti" claim)
     * @return compact serialized JWT string
     */
    public String generateToken(String userId, String email, String jwtId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000L);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .id(jwtId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Returns the expiry as an {@link Instant} for a freshly-issued token.
     * Used by the caller to set the Session.expiresAt in Neo4j.
     */
    public Instant getExpiryInstant() {
        return Instant.now().plusSeconds(expirationSeconds);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Validate token signature and expiry.
     *
     * @return {@code true} if valid; {@code false} on any failure (never throws).
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check whether the token has passed its expiry date.
     *
     * @return {@code true} if expired; {@code false} if still valid or on parse error.
     */
    public boolean isTokenExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    // ── Claim extraction ────────────────────────────────────────────────────

    /**
     * Extract the user's internal UUID (JWT subject claim).
     */
    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extract the JWT unique ID ("jti" claim) — used as Redis cache key and Neo4j sessionId.
     */
    public String extractJwtId(String token) {
        return parseClaims(token).getId();
    }

    /**
     * Extract the user's email address from the "email" claim.
     */
    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
