package com.app.auth.session.repository;

import com.app.auth.session.node.SessionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link SessionNode}.
 *
 * <p>Session creation uses a custom @Query that atomically creates the Session node
 * AND the HAS_SESSION relationship in one Cypher statement — avoiding the
 * need to model the relationship on UserNode (which would cause eager-loading issues).</p>
 */
@Repository
public interface SessionRepository extends Neo4jRepository<SessionNode, String> {

    /**
     * Find a session by its JWT "jti" value.
     */
    Optional<SessionNode> findBySessionId(String sessionId);

    /**
     * Atomically create a Session node and a HAS_SESSION edge from the User node.
     *
     * @param id       application-assigned UUID for the Session node
     * @param userId    internal UUID of the user (UserNode.id)
     * @param sessionId JWT jti claim value
     * @param issuedAt  creation timestamp (UTC)
     * @param expiresAt expiry timestamp (UTC)
     * @return the newly created SessionNode
     */
    @Query("MATCH (u:User {id: $userId}) " +
           "CREATE (s:Session {id: $id, sessionId: $sessionId, issuedAt: $issuedAt, " +
           "                   expiresAt: $expiresAt, revoked: false}) " +
           "CREATE (u)-[:HAS_SESSION]->(s) " +
           "RETURN s")
    SessionNode createSessionForUser(@Param("id") String id,
                                    @Param("userId") String userId,
                                    @Param("sessionId") String sessionId,
                                    @Param("issuedAt") LocalDateTime issuedAt,
                                    @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * Mark a single session as revoked (used on logout).
     */
    @Query("MATCH (s:Session {sessionId: $sessionId}) SET s.revoked = true RETURN s")
    Optional<SessionNode> revokeBySessionId(@Param("sessionId") String sessionId);

    /**
     * Atomically check if a session is valid (not revoked, not expired) AND revoke it.
     * Crucial for preventing race conditions during refresh rotation.
     */
    @Query("MATCH (s:Session {sessionId: $sessionId}) " +
           "SET s._lock = true " + // CHANGED: Acquire exclusive lock immediately to serialize concurrent refreshes
           "WITH s " +
           "WHERE s.revoked = false AND s.expiresAt > $now " +
           "SET s.revoked = true " +
           "REMOVE s._lock " +
           "RETURN s")
    Optional<SessionNode> findAndRevokeAtomic(@Param("sessionId") String sessionId,
                                             @Param("now") LocalDateTime now);

    /**
     * Mark ALL sessions for a user as revoked (used on global sign-out).
     */
    @Query("MATCH (u:User {id: $userId})-[:HAS_SESSION]->(s:Session) " +
           "SET s.revoked = true RETURN s")
    List<SessionNode> revokeAllSessionsForUser(@Param("userId") String userId);

    /**
     * Delete all expired sessions — useful for a scheduled cleanup job.
     */
    @Query("MATCH (s:Session) WHERE s.expiresAt < $now DETACH DELETE s")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);
}
