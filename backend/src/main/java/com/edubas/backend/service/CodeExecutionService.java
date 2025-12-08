package com.edubas.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.edubas.backend.dto.CodeExecutionResponse;
import com.edubas.backend.dto.TestCaseResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class CodeExecutionService {
    private static final String PISTON_API_URL = "https://emkc.org/api/v2/piston/execute";
    private static final Logger logger = LoggerFactory.getLogger(CodeExecutionService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CodeExecutionService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public CodeExecutionResponse executeCode(String language, String code, List<String> inputs,
            List<String> expectedOutputs) {

        // Execute test cases sequentially with rate limiting (1 request per 200ms)
        List<TestCaseResult> results = new ArrayList<>();

        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            String expected = i < expectedOutputs.size() ? expectedOutputs.get(i) : "";

            TestCaseResult result = executeTestCase(language, code, input, expected);
            results.add(result);

            // Add 200ms delay between requests to comply with rate limit
            if (i < inputs.size() - 1) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Rate limiting delay interrupted", e);
                }
            }
        }

        int passedCount = (int) results.stream().filter(TestCaseResult::isPassed).count();
        CodeExecutionResponse response = new CodeExecutionResponse();
        response.setResults(results);
        response.setPassedCount(passedCount);
        response.setTotalCount(results.size());

        return response;
    }

    private TestCaseResult executeTestCase(String language, String code, String input, String expected) {
        try {
            String fileExtension = getFileExtension(language);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("language", language);
            requestBody.put("version", getLanguageVersion(language));

            ArrayNode files = objectMapper.createArrayNode();
            ObjectNode file = objectMapper.createObjectNode();
            file.put("name", "solution." + fileExtension);
            file.put("content", code);
            files.add(file);

            requestBody.set("files", files);
            requestBody.put("stdin", input);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            String response = restTemplate.postForObject(PISTON_API_URL, entity, String.class);
            JsonNode responseNode = objectMapper.readTree(response);

            if (responseNode.has("run")) {
                JsonNode runNode = responseNode.get("run");
                String stdout = runNode.has("stdout") ? runNode.get("stdout").asText().trim() : "";
                String stderr = runNode.has("stderr") ? runNode.get("stderr").asText() : "";

                // Normalize both outputs - remove all extra whitespace and compare
                String normalizedStdout = stdout.replaceAll("\\s+", " ").trim();
                String normalizedExpected = expected.trim().replaceAll("\\s+", " ").trim();

                boolean passed = normalizedStdout.equalsIgnoreCase(normalizedExpected) ||
                        normalizedStdout.equals(normalizedExpected);

                return new TestCaseResult(
                        input,
                        expected,
                        stdout,
                        passed,
                        stderr.isEmpty() ? null : stderr);
            } else {
                return new TestCaseResult(
                        input,
                        expected,
                        null,
                        false,
                        responseNode.has("message") ? responseNode.get("message").asText() : "Execution failed");
            }
        } catch (Exception e) {
            logger.error("Error executing test case with input '{}': {}", input, e.getMessage(), e);
            return new TestCaseResult(
                    input,
                    expected,
                    null,
                    false,
                    "Error: " + e.getMessage());
        }
    }

    private String getFileExtension(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "java";
            case "python" -> "py";
            case "javascript" -> "js";
            case "typescript" -> "ts";
            case "cpp" -> "cpp";
            case "csharp" -> "cs";
            case "go" -> "go";
            case "rust" -> "rs";
            case "php" -> "php";
            case "ruby" -> "rb";
            case "kotlin" -> "kt";
            case "swift" -> "swift";
            default -> "txt";
        };
    }

    private String getLanguageVersion(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "15.0.2";
            case "python" -> "3.10.0";
            case "javascript" -> "18.15.0";
            case "typescript" -> "5.0.3";
            case "cpp", "c++" -> "10.2.0";
            case "csharp", "c#" -> "6.12.0";
            case "go" -> "1.16.2";
            case "rust" -> "1.68.2";
            case "php" -> "8.2.3";
            case "ruby" -> "3.0.1";
            case "kotlin" -> "1.8.20";
            case "swift" -> "5.3.3";
            default -> "1.0.0";
        };
    }
}
