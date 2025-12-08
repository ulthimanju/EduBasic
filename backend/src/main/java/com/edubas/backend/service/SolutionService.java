package com.edubas.backend.service;

import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SolutionService {
    private static final Logger logger = LoggerFactory.getLogger(SolutionService.class);

    @Autowired(required = false)
    private Driver driver;

    public String saveSolution(String userName, String problemId, String code, String language) {
        if (driver == null) {
            logger.warn("Neo4j driver not available. Solution not saved to database.");
            // Return a dummy ID when database is not available
            return "OFFLINE_" + System.currentTimeMillis();
        }

        String cypherQuery = """
                MERGE (u:User {username: $userName})
                MERGE (p:Problem {id: $problemId})
                CREATE (s:Solution {id: randomUUID(), code: $code, language: $language, submittedAt: datetime()})
                CREATE (s)-[:SOLUTION_SUBMITTED_BY]->(u)
                CREATE (s)-[:SOLVES]->(p)
                RETURN s.id as id
                """;

        try (Session session = driver.session()) {
            Result result = session.run(cypherQuery, Map.of(
                    "userName", userName,
                    "problemId", problemId,
                    "code", code,
                    "language", language));

            if (result.hasNext()) {
                String solutionId = result.next().get("id").asString();
                logger.info("Solution saved with ID: {}", solutionId);
                return solutionId;
            } else {
                throw new RuntimeException("Failed to create solution node");
            }
        } catch (Exception e) {
            logger.error("Error saving solution: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save solution: " + e.getMessage());
        }
    }
}
