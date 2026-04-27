package com.nexora.courseservice.security;

import com.nexora.courseservice.exception.CourseServiceException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExamServiceClientTest {

    private MockWebServer mockWebServer;
    private ExamServiceClient examServiceClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        
        examServiceClient = new ExamServiceClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void isExamPublished_ShouldReturnTrue_WhenStatusIsPublished() {
        // Arrange
        UUID examId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"id\":\"" + examId + "\", \"status\":\"PUBLISHED\"}")
                .addHeader("Content-Type", "application/json"));

        // Act
        boolean result = examServiceClient.isExamPublished(examId, "Bearer token");

        // Assert
        assertTrue(result);
    }

    @Test
    void isExamPublished_ShouldReturnFalse_WhenStatusIsDraft() {
        // Arrange
        UUID examId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"id\":\"" + examId + "\", \"status\":\"DRAFT\"}")
                .addHeader("Content-Type", "application/json"));

        // Act
        boolean result = examServiceClient.isExamPublished(examId, "Bearer token");

        // Assert
        assertFalse(result);
    }

    @Test
    void isExamPublished_ShouldThrowException_WhenNotFound() {
        // Arrange
        UUID examId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        // Act & Assert
        CourseServiceException ex = assertThrows(CourseServiceException.class, 
                () -> examServiceClient.isExamPublished(examId, "Bearer token"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("EXAM_NOT_FOUND", ex.getErrorCode());
    }
}
