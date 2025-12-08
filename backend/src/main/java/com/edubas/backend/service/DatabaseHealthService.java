package com.edubas.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class DatabaseHealthService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthService.class);

    private final Neo4jClient neo4jClient;

    public DatabaseHealthService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @PostConstruct
    public void logDatabaseConnectivity() {
        try {
            Integer result = neo4jClient.query("RETURN 1 AS ok")
                    .fetchAs(Integer.class)
                    .mappedBy((typeSystem, record) -> record.get("ok").asInt())
                    .one()
                    .orElse(0);
            if (result == 1) {
                logger.info("Neo4j connectivity check: OK");
            } else {
                logger.warn("Neo4j connectivity check: unexpected result {}", result);
            }
        } catch (Exception e) {
            logger.error("Neo4j connectivity check FAILED: {}", e.getMessage(), e);
        }
    }
}
