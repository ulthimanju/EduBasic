package com.app.auth.common.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Runs Neo4j schema migrations (UNIQUE constraints + indexes) on startup.
 * Uses IF NOT EXISTS so reruns are safe (idempotent).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaInitializer implements CommandLineRunner {

    private final Driver neo4jDriver;

    private static final String[] SCHEMA_STATEMENTS = {
        "CREATE CONSTRAINT user_id_unique IF NOT EXISTS " +
            "FOR (u:User) REQUIRE u.id IS UNIQUE",

        "CREATE CONSTRAINT user_googleId_unique IF NOT EXISTS " +
            "FOR (u:User) REQUIRE u.googleId IS UNIQUE",

        "CREATE CONSTRAINT user_email_unique IF NOT EXISTS " +
            "FOR (u:User) REQUIRE u.email IS UNIQUE",

        "CREATE CONSTRAINT session_id_unique IF NOT EXISTS " +
            "FOR (s:Session) REQUIRE s.id IS UNIQUE",

        "CREATE CONSTRAINT session_sessionId_unique IF NOT EXISTS " +
            "FOR (s:Session) REQUIRE s.sessionId IS UNIQUE",

        "CREATE INDEX session_expiresAt IF NOT EXISTS " +
            "FOR (s:Session) ON (s.expiresAt)"
    };

    @Override
    public void run(String... args) {
        log.info("Running Neo4j schema initialization...");
        try (Session session = neo4jDriver.session()) {
            for (String statement : SCHEMA_STATEMENTS) {
                session.run(statement);
                log.debug("Applied: {}", statement);
            }
            log.info("Neo4j schema initialization complete.");
        } catch (Exception e) {
            log.error("Neo4j schema initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException("Neo4j schema initialization failed", e);
        }
    }
}
