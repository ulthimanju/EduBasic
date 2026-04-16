package com.app.auth.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * Neo4j configuration.
 *
 * <p>Connection URI and credentials are set via {@code application.yml} which reads
 * environment variables (NEO4J_PASSWORD). No beans are declared here because
 * Spring Boot's auto-configuration provides all required beans:
 * <ul>
 *   <li>{@code Neo4jTransactionManager} (imperative)</li>
 *   <li>{@code ReactiveNeo4jTransactionManager} (reactive)</li>
 *   <li>{@code Neo4jClient} (for raw Cypher)</li>
 *   <li>{@code Driver} (Bolt driver)</li>
 * </ul>
 *
 * <p>Manually registering any of the above causes
 * {@code NoUniqueBeanDefinitionException} — Spring Boot already handles them.
 */
@Configuration
public class Neo4jConfig {
    // intentionally empty — Spring Boot auto-configuration handles everything
}
