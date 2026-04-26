package com.app.sandbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DockerExecutor {

    public List<Map<String, Object>> execute(String language, String sourceCode, JsonNode testCases, int timeLimitMs) {
        if (!"JAVA".equalsIgnoreCase(language)) {
            log.warn("Language {} not supported for actual execution yet, falling back to mock", language);
            return mockExecute(sourceCode, testCases);
        }

        log.info("Executing Java code with {}ms time limit", timeLimitMs);
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("sandbox-exec-");
            String className = extractClassName(sourceCode);
            Path sourceFile = tempDir.resolve(className + ".java");
            Files.writeString(sourceFile, sourceCode);

            // 1. Compile
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", sourceFile.toString());
            Process compileProcess = compileBuilder.start();
            boolean compiled = compileProcess.waitFor(10, TimeUnit.SECONDS);
            
            if (!compiled || compileProcess.exitValue() != 0) {
                String error = new String(compileProcess.getErrorStream().readAllBytes());
                return createErrorResults(testCases, "Compilation Error: " + error);
            }

            // 2. Execute Test Cases
            List<Map<String, Object>> results = new ArrayList<>();
            for (JsonNode tc : testCases) {
                results.add(runTestCase(tempDir, className, tc, timeLimitMs));
            }
            return results;

        } catch (Exception e) {
            log.error("Execution failed", e);
            return createErrorResults(testCases, "Internal Error: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir.toFile());
                } catch (Exception e) {
                    log.warn("Failed to delete temp dir: {}", tempDir);
                }
            }
        }
    }

    private Map<String, Object> runTestCase(Path dir, String className, JsonNode tc, int timeLimitMs) {
        String input = tc.has("input") ? tc.get("input").asText() : "";
        String expected = tc.get("expectedOutput").asText().trim();
        String testCaseId = tc.get("id").asText();
        boolean isHidden = tc.has("isHidden") && tc.get("isHidden").asBoolean();

        ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", dir.toString(), className);
        long start = System.currentTimeMillis();
        
        try {
            Process process = runBuilder.start();
            
            // Provide input
            if (!input.isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(input.getBytes());
                    os.flush();
                }
            }

            boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - start;

            if (!finished) {
                process.destroyForcibly();
                return Map.of(
                    "testCaseId", testCaseId,
                    "status", "TIME_LIMIT_EXCEEDED",
                    "actualOutput", "Time Limit Exceeded",
                    "executionTimeMs", (int)executionTime,
                    "isHidden", isHidden
                );
            }

            if (process.exitValue() != 0) {
                String error = new String(process.getErrorStream().readAllBytes());
                return Map.of(
                    "testCaseId", testCaseId,
                    "status", "RUNTIME_ERROR",
                    "actualOutput", error,
                    "executionTimeMs", (int)executionTime,
                    "isHidden", isHidden
                );
            }

            String output = new String(process.getInputStream().readAllBytes()).trim();
            boolean passed = output.equals(expected);

            return Map.of(
                "testCaseId", testCaseId,
                "status", passed ? "PASSED" : "FAILED",
                "actualOutput", output,
                "executionTimeMs", (int)executionTime,
                "isHidden", isHidden
            );

        } catch (Exception e) {
            return Map.of(
                "testCaseId", testCaseId,
                "status", "ERROR",
                "actualOutput", e.getMessage(),
                "executionTimeMs", 0,
                "isHidden", isHidden
            );
        }
    }

    private String extractClassName(String sourceCode) {
        Pattern pattern = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(sourceCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Main"; // Fallback
    }

    private List<Map<String, Object>> createErrorResults(JsonNode testCases, String errorMessage) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (JsonNode tc : testCases) {
            results.add(Map.of(
                "testCaseId", tc.get("id").asText(),
                "status", "ERROR",
                "actualOutput", errorMessage,
                "executionTimeMs", 0,
                "isHidden", tc.has("isHidden") && tc.get("isHidden").asBoolean()
            ));
        }
        return results;
    }

    private List<Map<String, Object>> mockExecute(String sourceCode, JsonNode testCases) {
        // ... (previous mock logic if needed for non-java)
        return createErrorResults(testCases, "Only Java is supported for execution currently.");
    }

    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }
}
