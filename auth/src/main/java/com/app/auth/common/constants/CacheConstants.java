package com.app.auth.common.constants;

/**
 * Redis key prefixes and TTL values.
 *
 * <p>All cache methods use these constants — never hardcode key strings elsewhere.</p>
 *
 * <p>Key patterns:
 * <pre>
 *   auth:jwt:{sessionId}  → "valid" | "invalid"   TTL = JWT_TTL_SECONDS
 *   auth:user:{userId}    → JSON(UserResponseDTO)  TTL = USER_TTL_SECONDS
 * </pre>
 */
public final class CacheConstants {

    private CacheConstants() {}

    public static final String AUTH_JWT_PREFIX  = "auth:jwt:";
    public static final String AUTH_USER_PREFIX = "auth:user:";

    /** TTL for JWT validity cache — must equal JWT expiration (1 hour). */
    public static final long JWT_TTL_SECONDS  = 3600L;

    /** Short TTL for positive (valid) JWT cache to reduce DB load while allowing revocation. */
    public static final long VALID_JWT_TTL_SECONDS = 60L;

    /** TTL for cached user profile (15 minutes — shorter than JWT to catch profile updates). */
    public static final long USER_TTL_SECONDS = 900L;

    /** Value stored when a JWT is confirmed valid. */
    public static final String JWT_VALID   = "valid";

    /**
     * Value stored when a JWT is confirmed invalid / revoked.
     * NOTE: This is a contract shared with the Exam service (TokenValidationService).
     * Do not change without updating both services.
     */
    public static final String JWT_INVALID = "invalid";
}
