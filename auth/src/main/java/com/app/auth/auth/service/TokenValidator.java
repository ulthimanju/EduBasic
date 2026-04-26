package com.app.auth.auth.service;

import com.app.auth.cache.service.CacheService;
import com.app.auth.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

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
     * Checks if a token is valid, considering cache and database (Neo4j).
     * If cache miss, it checks Neo4j and populates cache.
     */
    public boolean isTokenValid(String token, String jwtId) {
        // 1. Redis cache hit check
        java.util.Optional<Boolean> cached = cacheService.getJwtValidity(jwtId);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. Neo4j session revocation check
        if (!sessionService.isSessionValid(jwtId)) {
            invalidateToken(token, jwtId);
            return false;
        }

        // 3. Populate cache (optional: short TTL for valid tokens to reduce DB load)
        cacheService.cacheJwtValidity(jwtId, true);
        return true;
    }
}
