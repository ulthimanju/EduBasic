package com.edubas.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.edubas.backend.config.AppUrlConfig;
import com.edubas.backend.dto.PracticeProblemResponse;
import com.edubas.backend.dto.TestCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String modelName;

    @Autowired
    private AppUrlConfig appUrlConfig;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fix broken Mermaid diagram code using Gemini API
     *
     * @param brokenCode    The invalid Mermaid code
     * @param lessonTitle   The title of the lesson for context
     * @param lessonContext Brief description or theory for context
     * @return Fixed Mermaid code
     * @throws Exception if API call fails
     */
    public String fixMermaidCode(String brokenCode, String lessonTitle, String lessonContext) throws Exception {
        String prompt = buildPrompt(brokenCode, lessonTitle, lessonContext);

        // Build request body for Gemini API
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        requestBody.put("contents", List.of(content));

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Make API call with API key from application.properties
        String urlWithKey = appUrlConfig.getGeminiApiBaseUrl() + "/models/" + modelName
                + ":generateContent?key=" + apiKey;
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                urlWithKey,
                HttpMethod.POST,
                request,
                String.class);

        // Parse response
        if (response.getStatusCode() == HttpStatus.OK) {
            return extractFixedCode(response.getBody());
        } else {
            throw new Exception("Gemini API returned status: " + response.getStatusCode());
        }
    }

    /**
     * Generate a structured practice problem using Gemini given minimal lesson
     * context.
     *
     * @param levelType   e.g., Beginner, Intermediate, Advanced
     * @param moduleName  the module name
     * @param lessonTitle the lesson title
     * @return practice problem response with required fields filled when successful
     * @throws Exception if the Gemini call fails or the response cannot be parsed
     */
    public PracticeProblemResponse generatePracticeProblem(String courseTitle, String levelType, String moduleName,
            String lessonTitle)
            throws Exception {
        String prompt = buildPracticeProblemPrompt(courseTitle, levelType, moduleName, lessonTitle);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String urlWithKey = appUrlConfig.getGeminiApiBaseUrl() + "/models/" + modelName
                + ":generateContent?key=" + apiKey;
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                urlWithKey,
                HttpMethod.POST,
                request,
                String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            String rawText = extractText(response.getBody());
            return parsePracticeProblem(rawText);
        }

        throw new Exception("Gemini API returned status: " + response.getStatusCode());
    }

    private String buildPrompt(String brokenCode, String lessonTitle, String lessonContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(
                "You are a Mermaid diagram syntax expert. Fix the following broken Mermaid diagram code to be completely valid.\n\n");
        prompt.append("Lesson Title: ").append(lessonTitle).append("\n");

        if (lessonContext != null && !lessonContext.isEmpty()) {
            prompt.append("Context: ").append(lessonContext.substring(0, Math.min(lessonContext.length(), 200)))
                    .append("\n\n");
        }

        prompt.append("Broken Mermaid Code:\n");
        prompt.append(brokenCode).append("\n\n");

        prompt.append("Critical Instructions - READ CAREFULLY:\n");
        prompt.append("1. MUST produce 100% valid Mermaid flowchart syntax - test every line\n");
        prompt.append("2. ALL labels must be PLAIN TEXT ONLY - NO special characters\n");
        prompt.append(
                "3. FORBIDDEN characters in labels: ( ) , ? ! : ; ' \" { } [ ] & % $ # @ \\ / < > = + - * ^ ~ ` | \n");
        prompt.append("4. For edge labels (arrows), use ONLY letters, numbers, spaces, and basic words\n");
        prompt.append("5. Replace '&&' operator with '-->' (standard arrow)\n");
        prompt.append("6. Node syntax MUST be: NodeID[Label] where Label is simple text\n");
        prompt.append("7. Edge syntax MUST be: A --> B for simple edges or A -->|Label| B for labeled edges\n");
        prompt.append("8. NEVER use parentheses in labels - rephrase as simple words\n");
        prompt.append("9. Example: Change 'Yes (e.g., false or null)' to just 'Yes' or 'Yes false null'\n");
        prompt.append("10. Remove ALL quotes from labels - never use quotation marks inside labels\n");
        prompt.append("11. Use TOP-DOWN vertical alignment: Use 'flowchart TD' (not LR, BT, or other directions)\n");
        prompt.append("12. For vertical flowcharts, arrange nodes in clear hierarchical levels from top to bottom\n");
        prompt.append("13. Validate EVERY line for syntax errors before returning\n");
        prompt.append("14. Return ONLY the fixed code - NO markdown fences, NO explanations, NO comments\n");
        prompt.append("15. Start directly with diagram type (flowchart TD for vertical layout)\n\n");
        prompt.append("CORRECT SYNTAX EXAMPLES:\n");
        prompt.append("flowchart TD\n");
        prompt.append("    A[Start Process]\n");
        prompt.append("    B[Evaluate Condition]\n");
        prompt.append("    C{Is Valid}\n");
        prompt.append("    A --> B\n");
        prompt.append("    B --> C\n");
        prompt.append("    C -->|Yes| D[Continue]\n");
        prompt.append("    C -->|No| E[Stop]\n");
        prompt.append("\nINCORRECT - DO NOT USE:\n");
        prompt.append("    C -->|Yes (e.g., false or null)| D[Result]\n");
        prompt.append("    A[Label with 'quotes']\n");
        prompt.append("    B{Decision: check (status)?}\n");
        prompt.append("    flowchart LR (use TD for vertical alignment instead)\n");

        return prompt.toString();
    }

    private String extractFixedCode(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // Navigate through Gemini's response structure
        JsonNode candidates = root.get("candidates");
        if (candidates != null && candidates.isArray() && candidates.size() > 0) {
            JsonNode content = candidates.get(0).get("content");
            if (content != null) {
                JsonNode parts = content.get("parts");
                if (parts != null && parts.isArray() && parts.size() > 0) {
                    JsonNode text = parts.get(0).get("text");
                    if (text != null) {
                        String fixedCode = text.asText().trim();

                        // Remove any markdown code fences that might still be present
                        fixedCode = fixedCode.replaceAll("^```mermaid\\s*", "");
                        fixedCode = fixedCode.replaceAll("^```\\s*", "");
                        fixedCode = fixedCode.replaceAll("\\s*```$", "");

                        return fixedCode.trim();
                    }
                }
            }
        }

        throw new Exception("Could not extract fixed code from Gemini response");
    }

    private String buildPracticeProblemPrompt(String courseTitle, String levelType, String moduleName,
            String lessonTitle) {
        // Lock language to Java to avoid cross-language drift; can be parameterized
        // later if needed
        String targetLanguage = "Java";

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert Computer Science educator specialized in ").append(targetLanguage)
                .append(".\n");
        prompt.append("Draft a precise programming practice problem based strictly on the context below.\n\n");

        // Delimit context to reduce prompt-injection risk and keep scope tight
        prompt.append("<Context>\n");
        prompt.append("  <Course>")
                .append(courseTitle == null || courseTitle.isBlank() ? "General Course" : courseTitle)
                .append("</Course>\n");
        prompt.append("  <Level>").append(levelType == null || levelType.isBlank() ? "General" : levelType)
                .append("</Level>\n");
        prompt.append("  <Module>")
                .append(moduleName == null || moduleName.isBlank() ? "General Programming" : moduleName)
                .append("</Module>\n");
        prompt.append("  <Lesson>")
                .append(lessonTitle == null || lessonTitle.isBlank() ? "Concept Verification" : lessonTitle)
                .append("</Lesson>\n");
        prompt.append("</Context>\n\n");

        // Structured JSON schema with object-based test cases for safer parsing
        prompt.append("Output a single valid JSON object. Do not include markdown fences (no ```json).\n");
        prompt.append("Inside string fields you may use Markdown emphasis (e.g., **bold**) to highlight key terms.\n");
        prompt.append("The JSON must adhere to this schema:\n");
        prompt.append("{\n");
        prompt.append("  \"title\": \"String (Short, engaging title)\",\n");
        prompt.append(
                "  \"statement\": \"String (Problem description. Use \\\\n for newlines. Markdown emphasis allowed)\",\n");
        prompt.append(
                "  \"hints\": [\"String (Markdown emphasis allowed)\", \"String (Markdown emphasis allowed)\"],\n");
        prompt.append("  \"inputFormat\": \"String (Description of input; Markdown emphasis allowed)\",\n");
        prompt.append("  \"outputFormat\": \"String (Description of output; Markdown emphasis allowed)\",\n");
        prompt.append("  \"constraints\": \"String (e.g. '1 <= N <= 100')\",\n");
        prompt.append("  \"testCases\": [\n");
        prompt.append(
                "    { \"input\": \"String\", \"output\": \"String\", \"explanation\": \"String (optional)\" }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        prompt.append("Constraints:\n");
        prompt.append("- Ensure the problem is solvable within the provided level scope.\n");
        prompt.append("- Prefer clear variable names; no proprietary data or external links.\n");
        prompt.append("- Provide exactly 4 test cases with varying complexity.\n");
        prompt.append("- Return RAW JSON only (no prose, no code fences).\n");
        return prompt.toString();
    }

    private String extractText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.get("candidates");
        if (candidates != null && candidates.isArray() && candidates.size() > 0) {
            JsonNode content = candidates.get(0).get("content");
            if (content != null) {
                JsonNode parts = content.get("parts");
                if (parts != null && parts.isArray() && parts.size() > 0) {
                    JsonNode text = parts.get(0).get("text");
                    if (text != null) {
                        String cleaned = text.asText().trim();
                        cleaned = cleaned.replaceAll("^```json\\s*", "");
                        cleaned = cleaned.replaceAll("^```\\s*", "");
                        cleaned = cleaned.replaceAll("\\s*```$", "");
                        return cleaned.trim();
                    }
                }
            }
        }
        throw new Exception("Could not extract text from Gemini response");
    }

    private PracticeProblemResponse parsePracticeProblem(String rawText) {
        try {
            JsonNode node = objectMapper.readTree(rawText);

            List<String> hints = new ArrayList<>();
            if (node.has("hints") && node.get("hints").isArray()) {
                node.get("hints").forEach(h -> hints.add(h.asText()));
            }

            List<TestCase> testCases = new ArrayList<>();
            if (node.has("testCases") && node.get("testCases").isArray()) {
                node.get("testCases").forEach(tc -> {
                    if (tc.isObject()) {
                        String input = tc.path("input").asText("");
                        String output = tc.path("output").asText("");
                        String explanation = tc.path("explanation").asText(null);
                        testCases.add(new TestCase(input, output, explanation));
                    }
                });
            }

            return new PracticeProblemResponse(
                    true,
                    "Problem generated",
                    node.path("title").asText(null),
                    node.path("statement").asText(null),
                    hints,
                    node.path("inputFormat").asText(null),
                    node.path("outputFormat").asText(null),
                    testCases,
                    node.path("constraints").asText(null));
        } catch (Exception e) {
            return new PracticeProblemResponse(false, "Failed to parse Gemini response: " + e.getMessage(), null, null,
                    new ArrayList<>(), null, null, new ArrayList<>(), null);
        }
    }
}