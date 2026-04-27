package com.nexora.courseservice.security;

import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.constants.LogMessages;
import com.nexora.courseservice.exception.CourseServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
public class ExamServiceClient {

    private final WebClient webClient;

    public ExamServiceClient(WebClient examServiceWebClient) {
        this.webClient = examServiceWebClient;
    }

    public boolean isExamPublished(UUID examId, String bearerToken) {
        try {
            ExamStatusResponse response = webClient.get()
                    .uri("/api/v1/exams/{id}/status", examId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            resp -> Mono.error(new CourseServiceException(
                                    ErrorMessages.EXAM_NOT_FOUND,
                                    "EXAM_NOT_FOUND",
                                    HttpStatus.BAD_REQUEST)))
                    .bodyToMono(ExamStatusResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return response != null && "PUBLISHED".equals(response.status());
        } catch (CourseServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn(LogMessages.EXAM_SERVICE_UNREACHABLE, examId);
            throw new CourseServiceException(
                    ErrorMessages.EXAM_SERVICE_UNAVAILABLE,
                    "EXAM_SERVICE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private record ExamStatusResponse(UUID id, String status) {}
}
