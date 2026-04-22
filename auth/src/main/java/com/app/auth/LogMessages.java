package com.app.auth;

/**
 * Centrally managed log messages for the Auth service.
 */
public final class LogMessages {
    private LogMessages() {}

    public static final String NEW_USER_CREATED = "New user created: id={}, email={}, roles={}";
    public static final String RETURNING_USER_UPDATED = "Returning user updated: id={}, email={}, roles={}";
    public static final String UPDATED_ROLES_FOR_USER = "Updated roles for user {}: {}";
    public static final String ADMIN_CHECK_FAILED_NO_AUTH = "Admin check failed — no authentication";
    public static final String ADMIN_ALLOWLIST_EMPTY_ACCESS_DENIED = "Admin allowlist is empty — all management access denied";
    public static final String ADMIN_CHECK_FAILED_UNEXPECTED_PRINCIPAL = "Admin check failed — unexpected principal type: {}";
    public static final String ADMIN_CHECK_USER_EMAIL_GRANTED = "Admin check: userId={}, email={}, granted={}";
    public static final String ADMIN_CHECK_FAILED_USER_NOT_FOUND = "Admin check failed — user not found: userId={}";
    public static final String GET_ME_USER_ID = "GET /api/auth/me userId={}";
    public static final String RUNNING_SCHEMA_INITIALIZATION = "Running Neo4j schema initialization...";
    public static final String APPLIED_STATEMENT = "Applied: {}";
    public static final String SCHEMA_INITIALIZATION_COMPLETE = "Neo4j schema initialization complete.";
    public static final String SCHEMA_INITIALIZATION_FAILED = "Neo4j schema initialization failed: {}";
    public static final String SESSION_CREATED = "Session created: userId={}, jwtId={}";
    public static final String SESSION_REVOKED = "Session revoked: jwtId={}";
    public static final String REVOKED_SESSIONS_FOR_USER = "Revoked {} session(s) for userId={}";
    public static final String USER_NOT_FOUND = "User not found: {}";
    public static final String TOKEN_VALIDATION_ERROR = "Token validation error: {}";
    public static final String VALIDATION_ERROR = "Validation error: {}";
    public static final String ACCESS_DENIED = "Access denied: {}";
    public static final String UNHANDLED_EXCEPTION = "Unhandled exception: {}";
    public static final String OAUTH2_USER_LOADED_GOOGLE = "OAuth2 user loaded from Google: email={}";
    public static final String JWT_VALIDATION_FAILED = "JWT validation failed: {}";
    public static final String MALFORMED_JWT_CANNOT_EXTRACT_JTI = "Malformed JWT — cannot extract jti: {}";
    public static final String JWT_CACHED_INVALID = "JWT cached as invalid: jwtId={}";
    public static final String JWT_LOCAL_VALIDATION_FAILED = "JWT local validation failed: jwtId={}";
    public static final String JWT_SESSION_NOT_FOUND_REVOKED = "JWT session not found or revoked: jwtId={}";
    public static final String JWT_USER_NOT_EXISTS = "JWT references a user that no longer exists: userId={}, jwtId={}";
    public static final String OAUTH2_LOGIN_SUCCESS = "OAuth2 login success: email={}";
    public static final String AUTH_COOKIE_SET_REDIRECTING = "Auth cookie set, redirecting to dashboard: userId={}";
    public static final String LOGOUT_USER_SESSION = "Logout: userId={}, jwtId={}";
    public static final String LOGOUT_BEST_EFFORT_FAILED = "Logout best-effort: could not parse or revoke token — {}";
    public static final String REDIS_WRITE_FAILED = "Redis write failed for key={}: {}";
    public static final String REDIS_READ_FAILED = "Redis read failed for key={}: {}";
    public static final String REDIS_DELETE_FAILED = "Redis delete failed for key={}: {}";
    public static final String SERIALIZATION_FAILED = "Serialization failed for userId={}: {}";
    public static final String DESERIALIZATION_FAILED = "Deserialization failed for userId={}: {}";
}
