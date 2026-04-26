package com.app.auth.session.service;

import com.app.auth.session.node.SessionNode;

import java.time.Instant;

/**
 * Contract for session lifecycle operations.
 * Sessions are the graph edge between a User and a JWT token.
 */
public interface SessionService {

    /**
     * Create a Session node in Neo4j and link it to the user via HAS_SESSION.
     *
     * @param userId    internal UUID of the user (UserNode.id)
     * @param jwtId     JWT "jti" claim — unique per token
     * @param expiresAt token expiry — stored for cleanup queries
     * @return the persisted SessionNode
     */
    SessionNode createSession(String userId, String jwtId, Instant expiresAt);

    /**
     * Mark a single session as revoked by its JWT jti value.
     * Used on logout.
     */
    void revokeSession(String jwtId);

    /**
     * Mark ALL sessions for the given user as revoked.
     * Used on global sign-out or account compromise.
     */
    void revokeAllForUser(String userId);

    /**
     * Check if a session is valid in the database (not revoked and not expired).
     *
     * @param jwtId JWT jti claim
     * @return true if session exists and is active
     */
    boolean isSessionValid(String jwtId);

    /**
     * Atomically check if a session is valid and revoke it.
     *
     * @param jwtId JWT jti claim
     * @return Optional of the session if it was successfully revoked, empty if not found/invalid/already revoked
     */
    java.util.Optional<SessionNode> findAndRevokeAtomic(String jwtId);
}
