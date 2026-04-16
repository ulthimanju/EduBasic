package com.app.auth.session.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

/**
 * Neo4j graph node representing an active login session.
 *
 * <p>sessionId equals the JWT "jti" claim — this is the per-token revocation handle.
 * A (User)-[:HAS_SESSION]->(Session) relationship is created on login and its
 * revoked flag is set to true on logout.</p>
 *
 * <p>An index on expiresAt (see SchemaInitializer) enables efficient periodic cleanup
 * of expired sessions.</p>
 */
@Node("Session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionNode {

    @Id
    private String id;

    /** Equals the JWT "jti" claim — unique per issued token. */
    private String sessionId;

    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean revoked = false;
}
