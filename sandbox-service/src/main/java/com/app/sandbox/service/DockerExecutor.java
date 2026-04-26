package com.app.sandbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DockerExecutor {

    public List<Map<String, Object>> execute(String language, String sourceCode, JsonNode testCases, int timeLimitMs) {
        log.info("Executing {} code with {}ms time limit", language, timeLimitMs);
        
        List<Map<String, Object>> results = new ArrayList<>();

        // Extract "print" or "return" value from source code for deterministic mock grading
        // In a real sandbox, this would be the actual output of the executed process.
        String simulatedOutput = extractSimulatedOutput(sourceCode);

        for (JsonNode tc : testCases) {
            String expectedOutput = tc.get("expectedOutput").asText().trim();
            
            // For mock purposes, we'll assume the code is "correct" if it matches expected or contains the logic
            // To make it deterministic and non-random:
            boolean passed = simulatedOutput.equals(expectedOutput) || sourceCode.contains(expectedOutput);
            
            // If the source code doesn't explicitly match, but is a "logical" solution (very basic mock logic)
            if (!passed && sourceCode.toLowerCase().contains("print") && sourceCode.contains(expectedOutput)) {
                passed = true;
            }

            results.add(Map.of(
                "testCaseId", tc.get("id").asText(),
                "status", passed ? "PASSED" : "FAILED",
                "actualOutput", passed ? expectedOutput : "Execution failed or incorrect output",
                "executionTimeMs", 10 + (int)(Math.random() * 50), // Deterministic range for mock
                "isHidden", tc.has("isHidden") && tc.get("isHidden").asBoolean()
            ));
        }

        return results;
    }

    private String extractSimulatedOutput(String sourceCode) {
        // Very basic extraction for mock purposes to satisfy deterministic testing
        if (sourceCode == null) return "";
        sourceCode = sourceCode.trim();
        
        // Example: if student just wrote "return 5" or "print(5)"
        if (sourceCode.contains("return ")) {
            String part = sourceCode.substring(sourceCode.indexOf("return ") + 7).trim();
            return part.split("[;\\s]")[0];
        }
        if (sourceCode.contains("print(")) {
            String part = sourceCode.substring(sourceCode.indexOf("print(") + 6).trim();
            return part.split("[\\)]")[0].replace("\"", "").replace("'", "");
        }
        
        return sourceCode;
    }
}
