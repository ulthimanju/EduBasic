package com.app.exam.service;

import com.app.exam.domain.*;
import com.app.exam.dto.SubmitAttemptEvent;
import com.app.exam.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private StudentAttemptRepository attemptRepository;
    @Mock
    private StudentAnswerRepository answerRepository;
    @Mock
    private ExamQuestionMappingRepository mappingRepository;
    @Mock
    private EvaluationResultRepository resultRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EvaluationService evaluationService;

    private UUID attemptId;
    private StudentAttempt attempt;
    private Exam exam;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        attemptId = UUID.randomUUID();
        exam = new Exam();
        exam.setId(UUID.randomUUID());
        exam.setNegativeMarking(true);

        attempt = new StudentAttempt();
        attempt.setId(attemptId);
        attempt.setExam(exam);
        attempt.setStudentId(UUID.randomUUID());
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void evaluateAttempt_MCQ_Single_Correct() {
        Question q = new Question();
        q.setId(UUID.randomUUID());
        q.setType(QuestionType.MCQ_SINGLE);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("correctOptionId", "opt1");
        q.setPayload(payload);

        ExamQuestionMapping m = new ExamQuestionMapping();
        m.setQuestion(q);
        m.setMarks(new BigDecimal("10.0"));
        m.setNegMark(new BigDecimal("2.0"));

        StudentAnswer a = new StudentAnswer();
        a.setQuestion(q);
        a.setRawAnswer("opt1");

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(mappingRepository.findAllByExamIdOrderByOrderIndexAsc(any())).thenReturn(List.of(m));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(List.of(a));
        when(resultRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());

        evaluationService.evaluateAttempt(attemptId);

        assertEquals(new BigDecimal("10.0"), a.getMarksObtained());
        assertEquals("AUTO_GRADED_CORRECT", a.getEvaluationStatus());
    }

    @Test
    void evaluateAttempt_MCQ_Single_Incorrect() {
        Question q = new Question();
        q.setId(UUID.randomUUID());
        q.setType(QuestionType.MCQ_SINGLE);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("correctOptionId", "opt1");
        q.setPayload(payload);

        ExamQuestionMapping m = new ExamQuestionMapping();
        m.setQuestion(q);
        m.setMarks(new BigDecimal("10.0"));
        m.setNegMark(new BigDecimal("2.0"));

        StudentAnswer a = new StudentAnswer();
        a.setQuestion(q);
        a.setRawAnswer("wrong");

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(mappingRepository.findAllByExamIdOrderByOrderIndexAsc(any())).thenReturn(List.of(m));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(List.of(a));
        when(resultRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());

        evaluationService.evaluateAttempt(attemptId);

        assertEquals(new BigDecimal("-2.0"), a.getMarksObtained());
        assertEquals("AUTO_GRADED_INCORRECT", a.getEvaluationStatus());
    }

    @Test
    void evaluateAttempt_MCQ_Multi_OrderInsensitive() {
        Question q = new Question();
        q.setId(UUID.randomUUID());
        q.setType(QuestionType.MCQ_MULTI);
        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode correctIds = objectMapper.createArrayNode();
        correctIds.add("opt1").add("opt2");
        payload.set("correctOptionIds", correctIds);
        q.setPayload(payload);

        ExamQuestionMapping m = new ExamQuestionMapping();
        m.setQuestion(q);
        m.setMarks(new BigDecimal("10.0"));

        StudentAnswer a = new StudentAnswer();
        a.setQuestion(q);
        a.setRawAnswer("[\"opt2\", \"opt1\"]"); // Reordered

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(mappingRepository.findAllByExamIdOrderByOrderIndexAsc(any())).thenReturn(List.of(m));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(List.of(a));
        when(resultRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());

        evaluationService.evaluateAttempt(attemptId);

        assertEquals("AUTO_GRADED_CORRECT", a.getEvaluationStatus());
    }

    @Test
    void evaluateAttempt_Coding_TriggersSandbox() {
        Question q = new Question();
        q.setId(UUID.randomUUID());
        q.setType(QuestionType.CODING);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("testCases", objectMapper.createArrayNode());
        q.setPayload(payload);

        ExamQuestionMapping m = new ExamQuestionMapping();
        m.setQuestion(q);

        StudentAnswer a = new StudentAnswer();
        a.setQuestion(q);
        a.setRawAnswer("public class Main {}");

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(mappingRepository.findAllByExamIdOrderByOrderIndexAsc(any())).thenReturn(List.of(m));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(List.of(a));
        when(resultRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());

        evaluationService.evaluateAttempt(attemptId);

        assertEquals("PENDING_SANDBOX", a.getEvaluationStatus());
        // Since Kafka is sent after commit, we can't easily verify with mockito here 
        // without mocking TransactionSynchronizationManager.
    }

    @Test
    void evaluateAttempt_Subjective_ManualRequired() {
        Question q = new Question();
        q.setId(UUID.randomUUID());
        q.setType(QuestionType.SUBJECTIVE);

        ExamQuestionMapping m = new ExamQuestionMapping();
        m.setQuestion(q);

        StudentAnswer a = new StudentAnswer();
        a.setQuestion(q);

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(mappingRepository.findAllByExamIdOrderByOrderIndexAsc(any())).thenReturn(List.of(m));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(List.of(a));
        when(resultRepository.findByAttemptId(attemptId)).thenReturn(Optional.empty());

        evaluationService.evaluateAttempt(attemptId);

        assertEquals("PENDING_MANUAL", a.getEvaluationStatus());
    }
}
