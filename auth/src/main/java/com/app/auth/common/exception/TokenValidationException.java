package com.app.auth.common.exception;

/**
 * Thrown when JWT validation fails (invalid signature, expired, malformed, revoked).
 * Mapped to HTTP 401 by {@link com.app.auth.common.exception.GlobalExceptionHandler}.
 */
public class TokenValidationException extends RuntimeException {

    public TokenValidationException(String message) {
        super(message);
    }
}
