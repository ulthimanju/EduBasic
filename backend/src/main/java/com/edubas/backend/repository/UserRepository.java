package com.edubas.backend.repository;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import com.edubas.backend.entity.User;

@Repository
public interface UserRepository extends Neo4jRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("MATCH (u:User) WHERE u.username = $username OR u.email = $email RETURN u")
    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
