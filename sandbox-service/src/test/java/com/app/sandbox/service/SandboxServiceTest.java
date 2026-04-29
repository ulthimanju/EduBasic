package com.app.sandbox.service;

import com.app.sandbox.domain.CodeSubmission;
import com.app.sandbox.repository.CodeSubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SandboxServiceTest {

    @Mock
    private CodeSubmissionRepository submissionRepository;

    @Mock
    private DockerExecutor dockerExecutor;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SandboxService sandboxService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lenient().when(kafkaTemplate.send(anyString(), any(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mock(org.springframework.kafka.support.SendResult.class)));
    }

    @Test
    void testConsumeSubmission_Success() {
        UUID attemptId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
            "attemptId", attemptId.toString(),
            "questionId", questionId.toString(),
            "language", "JAVA",
            "sourceCode", "code",
            "testCases", List.of(Map.of("id", "tc1", "expectedOutput", "out"))
        );
        Acknowledgment ack = mock(Acknowledgment.class);

        CodeSubmission submission = new CodeSubmission();
        submission.setAttemptId(attemptId);
        submission.setQuestionId(questionId);
        submission.setLanguage("JAVA");
        submission.setSourceCode("code");
        
        when(submissionRepository.findByAttemptIdAndQuestionId(attemptId, questionId)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(CodeSubmission.class))).thenReturn(submission);
        when(dockerExecutor.executeAsync(anyString(), anyString(), any(), anyInt()))
            .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(List.of(Map.of("testCaseId", "tc1", "status", "PASSED"))));

        sandboxService.consumeSubmission(event, ack);

        // 1. Initial save (QUEUED)
        // 2. Status update to RUNNING
        // 3. Final update to COMPLETED
        verify(submissionRepository, times(3)).save(any(CodeSubmission.class));
        verify(kafkaTemplate).send(eq("coding-result"), anyString(), anyMap());
        verify(ack).acknowledge();
    }

    @Test
    void testConsumeSubmission_DuplicateIdempotency() {
        UUID attemptId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
            "attemptId", attemptId.toString(),
            "questionId", questionId.toString()
        );
        Acknowledgment ack = mock(Acknowledgment.class);

        CodeSubmission existing = new CodeSubmission();
        existing.setAttemptId(attemptId);
        existing.setQuestionId(questionId);
        existing.setStatus("COMPLETED");
        existing.setTestCaseResults(List.of(Map.of("status", "PASSED")));
        existing.setOverallStatus("PASSED");
        
        when(submissionRepository.findByAttemptIdAndQuestionId(attemptId, questionId)).thenReturn(Optional.of(existing));

        sandboxService.consumeSubmission(event, ack);

        // Should NOT call save or dockerExecutor
        verify(submissionRepository, never()).save(any());
        verify(dockerExecutor, never()).executeAsync(any(), any(), any(), anyInt());
        
        // SHOULD still publish result and acknowledge
        verify(kafkaTemplate).send(eq("coding-result"), anyString(), anyMap());
        verify(ack).acknowledge();
    }
}
