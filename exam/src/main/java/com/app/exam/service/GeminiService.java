package com.app.exam.service;

import com.app.exam.domain.Question;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String modelName;

    @Value("${gemini.api-url}")
    private String apiUrl;

    @SuppressWarnings("unchecked")
    public List<Question> generateQuestions(String courseName, List<String> previousTopics, String difficulty, int count) {
        String topicsStr = previousTopics != null ? String.join(", ", previousTopics) : "None";
        
        String prompt = """
            You are an exam question generator.
            Generate %d MCQ questions for the course '%s' at difficulty '%s'.
            Previously covered topics: %s.
            Respond ONLY with a valid JSON array. Each object must have:
            { "question": "the question text", 
              "options": ["A","B","C","D"],
              "correctAnswer": "A", 
              "explanation": "why A is correct", 
              "topic": "the specific topic", 
              "difficulty": "%s" }
            """.formatted(count, courseName, difficulty, topicsStr, difficulty);

        try {
            String fullUrl = apiUrl + modelName + ":generateContent";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-goog-api-key", apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("Generating {} questions for {} using Gemini at {}", count, courseName, fullUrl);
            
            Map<String, Object> response = restTemplate.postForObject(fullUrl, entity, Map.class);
            
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    Map<String, Object> contentRes = (Map<String, Object>) firstCandidate.get("content");
                    List<Map<String, Object>> partsRes = (List<Map<String, Object>>) contentRes.get("parts");
                    if (!partsRes.isEmpty()) {
                        String text = (String) partsRes.get(0).get("text");
                        // Sometimes Gemini wraps JSON in markdown blocks like ```json ... ```
                        String cleanedJson = text.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "").trim();
                        return objectMapper.readValue(cleanedJson, new TypeReference<List<Question>>() {});
                    }
                }
            }
            
            return List.of(); 
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            return List.of();
        }
    }
}

