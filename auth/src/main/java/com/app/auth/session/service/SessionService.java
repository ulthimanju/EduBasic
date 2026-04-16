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
}
