package com.app.auth.user.repository;

import com.app.auth.user.node.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link UserNode}.
 * All query methods are derived or annotated Cypher — no hand-written JDBC.
 */
@Repository
public interface UserRepository extends Neo4jRepository<UserNode, String> {

    /**
     * Find user by their stable Google "sub" claim.
     * This is the primary lookup key for login (not email — email can change).
     */
    Optional<UserNode> findByGoogleId(String googleId);

    /**
     * Find user by email address.
     * Used for uniqueness checks and administrative lookups only.
     */
    Optional<UserNode> findByEmail(String email);
}
