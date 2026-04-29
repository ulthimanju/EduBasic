package com.app.exam.service;

import com.app.exam.LogMessages;
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

    private record GeminiResponse(List<Candidate> candidates) {}
    private record Candidate(Content content) {}
    private record Content(List<Part> parts) {}
    private record Part(String text) {}

    @org.springframework.scheduling.annotation.Async("geminiExecutor")
    public java.util.concurrent.CompletableFuture<List<Question>> generateQuestions(String courseName, List<String> previousTopics, String difficulty, int count) {
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
            
            log.info(LogMessages.GENERATING_QUESTIONS_GEMINI, count, courseName, fullUrl);
            
            String responseStr = restTemplate.postForObject(fullUrl, entity, String.class);
            GeminiResponse response = objectMapper.readValue(responseStr, GeminiResponse.class);
            
            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                Candidate candidate = response.candidates().get(0);
                if (candidate.content() != null && candidate.content().parts() != null && !candidate.content().parts().isEmpty()) {
                    String text = candidate.content().parts().get(0).text();
                    if (text != null) {
                        // Sometimes Gemini wraps JSON in markdown blocks like ```json ... ```
                        String cleanedJson = text.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "").trim();
                        return java.util.concurrent.CompletableFuture.completedFuture(objectMapper.readValue(cleanedJson, new TypeReference<List<Question>>() {}));
                    }
                }
            }
            
            return java.util.concurrent.CompletableFuture.completedFuture(List.of()); 
        } catch (Exception e) {
            log.error(LogMessages.ERROR_CALLING_GEMINI_API, e.getMessage());
            return java.util.concurrent.CompletableFuture.completedFuture(List.of());
        }
    }
}

