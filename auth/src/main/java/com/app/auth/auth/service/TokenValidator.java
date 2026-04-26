package com.app.auth.auth.service;

import com.app.auth.cache.service.CacheService;
import com.app.auth.common.constants.CacheConstants;
import com.app.auth.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenValidator {

    private final JwtService jwtService;
    private final CacheService cacheService;
    private final SessionService sessionService;

    /**
     * Mark a token as invalid in Redis for its remaining lifetime.
     */
    public void invalidateToken(String token, String jwtId) {
        try {
            Instant expiry = jwtService.extractExpiration(token);
            long remaining = Duration.between(Instant.now(), expiry).toSeconds();
            if (remaining > 0) {
                cacheService.cacheJwtValidity(jwtId, false, remaining);
            } else {
                cacheService.evictJwtCache(jwtId);
            }
        } catch (Exception e) {
            log.warn("Failed to calculate remaining TTL for token {}: {}", jwtId, e.getMessage());
            // Fallback to default TTL if parsing fails
            cacheService.cacheJwtValidity(jwtId, false);
        }
    }

    /**
     * Checks if an Access Token is valid.
     * Uses a short-lived cache for positive (valid) results to reduce DB load.
     * Always rejects immediately if cached as invalid.
     */
    public boolean isAccessTokenValid(String token, String jwtId) {
        // 1. Redis cache check
        Optional<Boolean> cached = cacheService.getJwtValidity(jwtId);
        if (cached.isPresent()) {
            if (!cached.get()) return false; // Definitely revoked
            return true; // Trust "valid" for the short cache duration (60s)
        }

        // 2. Neo4j authoritative check
        if (!sessionService.isSessionValid(jwtId)) {
            invalidateToken(token, jwtId);
            return false;
        }

        // 3. Cache "valid" for a short time
        cacheService.cacheJwtValidity(jwtId, true, CacheConstants.VALID_JWT_TTL_SECONDS);
        return true;
    }

    /**
     * Checks if a Refresh Token is valid.
     * Authoritative: always checks Neo4j if not already cached as invalid.
     * Does NOT trust positive cache hits.
     */
    public boolean isRefreshTokenValid(String token, String jwtId) {
        // 1. Redis cache check - only trust "invalid"
        Optional<Boolean> cached = cacheService.getJwtValidity(jwtId);
        if (cached.isPresent() && !cached.get()) {
            return false;
        }

        // 2. Neo4j authoritative check
        if (!sessionService.isSessionValid(jwtId)) {
            invalidateToken(token, jwtId);
            return false;
        }

        return true;
    }
}
