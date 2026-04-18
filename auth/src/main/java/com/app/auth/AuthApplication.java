package com.app.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Application entry point.
 *
 * <p>Reactive Neo4j auto-configuration is excluded because this application
 * is purely imperative (servlet-based, not reactive). Leaving it enabled causes
 * {@code NoUniqueBeanDefinitionException} when Spring resolves {@code @Transactional}
 * — it finds both {@code Neo4jTransactionManager} and {@code ReactiveNeo4jTransactionManager}
 * and cannot choose between them.</p>
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveDataAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveRepositoriesAutoConfiguration.class
})
@EnableCaching
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
