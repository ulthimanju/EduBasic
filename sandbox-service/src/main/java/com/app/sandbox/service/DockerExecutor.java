package com.app.sandbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class DockerExecutor {

    public List<Map<String, Object>> execute(String language, String sourceCode, JsonNode testCases, int timeLimitMs) {
        log.info("Executing {} code with {}ms time limit", language, timeLimitMs);
        
        List<Map<String, Object>> results = new ArrayList<>();
        Random random = new Random();

        for (JsonNode tc : testCases) {
            // Simulation of execution
            boolean passed = random.nextBoolean();
            int executionTime = random.nextInt(timeLimitMs / 2) + 10;
            
            results.add(Map.of(
                "testCaseId", tc.get("id").asText(),
                "status", passed ? "PASSED" : "FAILED",
                "actualOutput", passed ? tc.get("expectedOutput").asText() : "error_output",
                "executionTimeMs", executionTime,
                "isHidden", tc.has("isHidden") && tc.get("isHidden").asBoolean()
            ));
        }

        return results;
    }
}
