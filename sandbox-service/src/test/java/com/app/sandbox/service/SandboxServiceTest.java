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
        
        when(submissionRepository.save(any(CodeSubmission.class))).thenReturn(submission);
        when(dockerExecutor.execute(anyString(), anyString(), any(), anyInt()))
            .thenReturn(List.of(Map.of("testCaseId", "tc1", "status", "PASSED")));

        sandboxService.consumeSubmission(event, ack);

        verify(submissionRepository, times(2)).save(any(CodeSubmission.class));
        verify(kafkaTemplate).send(eq("coding-result"), anyString(), anyMap());
        verify(ack).acknowledge();
    }
}
