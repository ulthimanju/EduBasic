package com.app.auth.cache.service;

import com.app.auth.user.dto.UserResponseDTO;

import java.util.Optional;

/**
 * Contract for Redis cache operations.
 *
 * <p>Atomic rule: each method touches exactly one Redis key.
 * Serialization errors are caught internally — never crash the auth flow.</p>
 */
public interface CacheService {

    // ── JWT validity cache (auth:jwt:{sessionId}) ─────────────────────────────

    /**
     * Cache the validity state of a JWT by its jti (session ID).
     *
     * @param sessionId JWT jti claim
     * @param valid     {@code true} → store "valid"; {@code false} → store "invalid"
     */
    void cacheJwtValidity(String sessionId, boolean valid);

    /**
     * Retrieve the cached validity of a JWT.
     *
     * @return Optional with {@code true} (valid) / {@code false} (invalid),
     *         or {@link Optional#empty()} on cache miss.
     */
    Optional<Boolean> getJwtValidity(String sessionId);

    /**
     * Remove the JWT validity entry from cache (on logout / revocation).
     */
    void evictJwtCache(String sessionId);

    // ── User profile cache (auth:user:{userId}) ───────────────────────────────

    /**
     * Cache a serialised {@link UserResponseDTO} for the given userId.
     */
    void cacheUserProfile(String userId, UserResponseDTO user);

    /**
     * Retrieve a cached user profile.
     *
     * @return Optional containing the DTO, or empty on cache miss.
     */
    Optional<UserResponseDTO> getCachedUserProfile(String userId);

    /**
     * Remove the user profile from cache (on logout or profile change).
     */
    void evictUserCache(String userId);
}
