package com.app.exam.service;

import com.app.exam.domain.*;
import com.app.exam.dto.AttemptResponse;
import com.app.exam.dto.SyncAttemptRequest;
import com.app.exam.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    @Mock
    private StudentAttemptRepository attemptRepository;
    @Mock
    private StudentAnswerRepository answerRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ExamQuestionMappingRepository mappingRepository;
    @Mock
    private ExamSnapshotRepository snapshotRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AttemptService attemptService;

    private UUID studentId;
    private UUID examId;
    private Exam exam;
    private UUID attemptId;
    private StudentAttempt attempt;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        examId = UUID.randomUUID();
        attemptId = UUID.randomUUID();

        exam = new Exam();
        exam.setId(examId);
        exam.setStatus(ExamStatus.PUBLISHED);
        exam.setCurrentVersion(1);
        exam.setMaxAttempts(2);

        attempt = new StudentAttempt();
        attempt.setId(attemptId);
        attempt.setStudentId(studentId);
        attempt.setExam(exam);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setVersion(0);
    }

    @Test
    void startAttempt_Success() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(attemptRepository.findByStudentIdAndExamIdAndStatus(studentId, examId, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByStudentIdAndExamId(studentId, examId)).thenReturn(0L);
        
        ExamSnapshot snapshot = new ExamSnapshot();
        when(snapshotRepository.findByExamIdAndVersion(examId, 1)).thenReturn(Optional.of(snapshot));
        
        when(attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        AttemptResponse response = attemptService.startAttempt(studentId, examId);

        assertNotNull(response);
        verify(attemptRepository).save(any());
        verify(hashOperations).putAll(anyString(), anyMap());
    }

    @Test
    void startAttempt_Fail_NotPublished() {
        exam.setStatus(ExamStatus.DRAFT);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertThrows(RuntimeException.class, () -> attemptService.startAttempt(studentId, examId));
    }

    @Test
    void startAttempt_ReuseInProgress() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(attemptRepository.findByStudentIdAndExamIdAndStatus(studentId, examId, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));

        AttemptResponse response = attemptService.startAttempt(studentId, examId);

        assertEquals(attemptId, response.getId());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void startAttempt_Fail_MaxAttemptsReached() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(attemptRepository.findByStudentIdAndExamIdAndStatus(studentId, examId, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByStudentIdAndExamId(studentId, examId)).thenReturn(2L);

        assertThrows(RuntimeException.class, () -> attemptService.startAttempt(studentId, examId));
    }

    @Test
    void syncAttempt_Success_RedisOnly() {
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SyncAttemptRequest request = new SyncAttemptRequest();
        request.setVersion(0);
        request.setAnswers(Map.of(UUID.randomUUID(), "Answer"));

        attemptService.syncAttempt(studentId, attemptId, request);

        verify(hashOperations).put(anyString(), eq("answers"), any());
        verify(answerRepository, never()).saveAll(any());
    }

    @Test
    void syncAttempt_Success_FlushToPostgres() {
        attempt.setVersion(5);
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(Collections.emptyList());

        SyncAttemptRequest request = new SyncAttemptRequest();
        request.setVersion(5);
        UUID qId = UUID.randomUUID();
        request.setAnswers(Map.of(qId, "Answer"));

        when(mappingRepository.existsByExamIdAndQuestionId(examId, qId)).thenReturn(true);
        when(questionRepository.getReferenceById(qId)).thenReturn(new Question());

        attemptService.syncAttempt(studentId, attemptId, request);

        verify(answerRepository).saveAll(any());
    }

    @Test
    void syncAttempt_Fail_WrongStudent() {
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        
        SyncAttemptRequest request = new SyncAttemptRequest();
        assertThrows(RuntimeException.class, () -> attemptService.syncAttempt(UUID.randomUUID(), attemptId, request));
    }

    @Test
    void syncAttempt_Fail_VersionMismatch() {
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        
        SyncAttemptRequest request = new SyncAttemptRequest();
        request.setVersion(1); // Current is 0
        assertThrows(RuntimeException.class, () -> attemptService.syncAttempt(studentId, attemptId, request));
    }

    @Test
    void syncAttempt_Security_RejectQuestionNotInExam() {
        attempt.setVersion(5);
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(Collections.emptyList());

        SyncAttemptRequest request = new SyncAttemptRequest();
        request.setVersion(5);
        UUID invalidQId = UUID.randomUUID();
        request.setAnswers(Map.of(invalidQId, "Answer"));

        when(mappingRepository.existsByExamIdAndQuestionId(examId, invalidQId)).thenReturn(false);

        attemptService.syncAttempt(studentId, attemptId, request);

        // Verify that saveAll was called with an empty list or not at all
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository, atMostOnce()).saveAll(captor.capture());
        if (!captor.getAllValues().isEmpty()) {
            assertTrue(captor.getValue().isEmpty());
        }
    }

    @Test
    void syncAttempt_Abuse_RejectLargeAnswer() {
        attempt.setVersion(5);
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(Collections.emptyList());

        SyncAttemptRequest request = new SyncAttemptRequest();
        request.setVersion(5);
        UUID qId = UUID.randomUUID();
        
        char[] largeChars = new char[50001];
        Arrays.fill(largeChars, 'a');
        String largeAnswer = new String(largeChars);
        
        request.setAnswers(Map.of(qId, largeAnswer));
        when(mappingRepository.existsByExamIdAndQuestionId(examId, qId)).thenReturn(true);

        attemptService.syncAttempt(studentId, attemptId, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StudentAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository, atMostOnce()).saveAll(captor.capture());
        if (!captor.getAllValues().isEmpty()) {
            assertTrue(captor.getValue().isEmpty());
        }
    }

    @Test
    void submitAttempt_Success() {
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(anyString(), eq("answers"))).thenReturn(null); // Simple case

        attemptService.submitAttempt(studentId, attemptId);

        assertEquals(AttemptStatus.SUBMITTED, attempt.getStatus());
        verify(kafkaTemplate).send(eq("exam-submitted"), anyString(), any());
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void submitAttempt_Idempotent() {
        attempt.setStatus(AttemptStatus.SUBMITTED);
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        
        attemptService.submitAttempt(studentId, attemptId);

        // Should not try to resubmit or clear Redis again
        verify(kafkaTemplate, never()).send(eq("exam-submitted"), anyString(), any());
    }
}
