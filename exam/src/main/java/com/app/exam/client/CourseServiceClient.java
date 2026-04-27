package com.app.exam.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseServiceClient {

    private final RestTemplate restTemplate;

    @Value("${course.service.url:http://course-service:8083}")
    private String courseServiceUrl;

    public boolean validateStudentAccess(UUID studentId, UUID examId, String bearerToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = UriComponentsBuilder.fromUriString(courseServiceUrl)
                    .path("/api/v1/internal/courses/validate-access")
                    .queryParam("studentId", studentId)
                    .queryParam("examId", examId)
                    .toUriString();

            Boolean hasAccess = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class).getBody();
            return Boolean.TRUE.equals(hasAccess);
        } catch (Exception e) {
            log.error("Failed to validate student access with course-service", e);
            return false;
        }
    }
}
