package com.app.exam.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class CodingSandboxService {

    /**
     * Simulates code execution in a secure sandbox.
     * In a production environment, this would use Docker or a dedicated service like Judge0.
     */
    public boolean evaluate(String code, JsonNode testCases) {
        log.info("Simulating execution for code: {}...", code.substring(0, Math.min(code.length(), 20)));
        
        // Mocking sandbox execution logic
        // For demonstration, we assume student code passes if it's not empty
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        // Randomly simulate pass/fail if test cases are present
        if (testCases != null && testCases.isArray() && testCases.size() > 0) {
            return new Random().nextBoolean();
        }

        return true;
    }
}
