package com.app.auth.user.repository;

import com.app.auth.user.node.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends Neo4jRepository<UserNode, String> {

    Optional<UserNode> findByGoogleId(String googleId);

    /**
     * Atomic upsert via Cypher MERGE.
     * Prevents race conditions during simultaneous logins for new users.
     */
    @Query("MERGE (u:User {googleId: $googleId}) " +
           "ON CREATE SET u.id = $id, u.email = $email, u.name = $name, " +
           "              u.roles = $roles, u.createdAt = $now, u.lastLogin = $now " +
           "ON MATCH SET  u.email = $email, u.name = $name, u.lastLogin = $now " +
           "RETURN u")
    UserNode upsertUser(@Param("id") String id,
                        @Param("googleId") String googleId,
                        @Param("email") String email,
                        @Param("name") String name,
                        @Param("roles") java.util.Set<String> roles,
                        @Param("now") LocalDateTime now);
}
