package com.app.exam.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
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

    public boolean validateStudentAccess(UUID examId, String bearerToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = UriComponentsBuilder.fromUriString(courseServiceUrl)
                    .path("/api/v1/internal/courses/validate-access")
                    .queryParam("examId", examId)
                    .toUriString();

            Boolean hasAccess = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class).getBody();
            return Boolean.TRUE.equals(hasAccess);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.warn("Access denied by course-service for exam {}: {}", examId, e.getResponseBodyAsString());
                return false;
            }
            log.error("Course-service error {} while validating access for exam {}: {}", 
                    e.getStatusCode(), examId, e.getResponseBodyAsString());
            throw new RuntimeException("Course-service unavailable", e);
        } catch (Exception e) {
            log.error("Failed to connect to course-service for access validation for exam {}", examId, e);
            throw new RuntimeException("Course-service connection failed", e);
        }
    }
}
