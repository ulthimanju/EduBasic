package com.app.auth.common.exception;

/**
 * Thrown when a requested user is not found in Neo4j.
 * Mapped to HTTP 404 by {@link com.app.auth.common.exception.GlobalExceptionHandler}.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
