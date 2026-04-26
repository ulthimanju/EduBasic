package com.app.exam.service;

import com.app.exam.domain.*;
import com.app.exam.dto.SubmitAttemptEvent;
import com.app.exam.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final StudentAttemptRepository attemptRepository;
    private final StudentAnswerRepository answerRepository;
    private final ExamQuestionMappingRepository mappingRepository;
    private final EvaluationResultRepository resultRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void evaluateAttempt(UUID attemptId) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found: " + attemptId));

        log.info("Starting evaluation for attempt: {}", attemptId);

        List<ExamQuestionMapping> mappings = mappingRepository.findAllByExamIdOrderByOrderIndexAsc(attempt.getExam().getId());
        
        BigDecimal totalScore = BigDecimal.ZERO;
        boolean needsManualEvaluation = false;
        boolean hasPendingAsync = false;

        EvaluationResult result = resultRepository.findByAttemptId(attemptId)
                .orElseGet(() -> {
                    EvaluationResult r = new EvaluationResult();
                    r.setAttempt(attempt);
                    r.setStatus(EvaluationStatus.PENDING_AUTO);
                    return r;
                });

        for (ExamQuestionMapping mapping : mappings) {
            Question question = mapping.getQuestion();
            Optional<StudentAnswer> answerOpt = answerRepository.findByAttemptIdAndQuestionId(attemptId, question.getId());
            
            if (question.getType() == QuestionType.CODING) {
                if (answerOpt.isPresent()) {
                    hasPendingAsync = true;
                    sendToSandbox(attempt, question, answerOpt.get(), mapping);
                }
            } else if (question.getType() == QuestionType.SUBJECTIVE) {
                needsManualEvaluation = true;
                if (answerOpt.isPresent()) {
                    StudentAnswer answer = answerOpt.get();
                    answer.setEvaluationStatus("PENDING_MANUAL");
                    answerRepository.save(answer);
                }
            } else {
                if (answerOpt.isPresent()) {
                    BigDecimal score = autoEvaluate(question, answerOpt.get(), mapping.getMarks(), attempt.getExam().isNegativeMarking() ? mapping.getNegMark() : BigDecimal.ZERO);
                    totalScore = totalScore.add(score);
                }
            }
        }

        result.setTotalScore(totalScore);
        result.setStatus(hasPendingAsync ? EvaluationStatus.PENDING_AUTO : (needsManualEvaluation ? EvaluationStatus.PENDING_MANUAL : EvaluationStatus.COMPLETED));
        result.setEvaluatedAt(OffsetDateTime.now());
        resultRepository.save(result);

        attempt.setScore(totalScore);
        if (result.getStatus() == EvaluationStatus.COMPLETED) {
            attempt.setStatus(AttemptStatus.EVALUATED);
            kafkaTemplate.send("evaluation-completed", attemptId.toString(), new SubmitAttemptEvent(attemptId, attempt.getStudentId(), attempt.getExam().getId()));
        } else if (result.getStatus() == EvaluationStatus.PENDING_MANUAL) {
            kafkaTemplate.send("evaluation-needs-manual", attemptId.toString(), new SubmitAttemptEvent(attemptId, attempt.getStudentId(), attempt.getExam().getId()));
        }
        attemptRepository.save(attempt);
    }

    private void sendToSandbox(StudentAttempt attempt, Question question, StudentAnswer answer, ExamQuestionMapping mapping) {
        Map<String, Object> event = Map.of(
            "attemptId", attempt.getId().toString(),
            "questionId", question.getId().toString(),
            "language", "JAVA", // Default or extract from question payload
            "sourceCode", answer.getRawAnswer(),
            "testCases", question.getPayload().get("testCases"),
            "timeLimitMs", 2000
        );
        kafkaTemplate.send("coding-submitted", attempt.getId().toString(), event);
        answer.setEvaluationStatus("PENDING_SANDBOX");
        answerRepository.save(answer);
    }

    @KafkaListener(topics = "coding-result", groupId = "evaluation-group")
    @Transactional
    public void consumeCodingResult(Map<String, Object> event) {
        UUID attemptId = UUID.fromString((String) event.get("attemptId"));
        UUID questionId = UUID.fromString((String) event.get("questionId"));
        double scorePercent = (double) event.get("scorePercent");

        log.info("Received coding result for attempt: {}, question: {}", attemptId, questionId);

        StudentAnswer answer = answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));

        ExamQuestionMapping mapping = mappingRepository.findAllByExamIdOrderByOrderIndexAsc(answer.getAttempt().getExam().getId())
                .stream().filter(m -> m.getQuestion().getId().equals(questionId)).findFirst().get();

        BigDecimal marks = mapping.getMarks().multiply(BigDecimal.valueOf(scorePercent / 100.0));
        answer.setMarksObtained(marks);
        answer.setEvaluationStatus("AUTO_GRADED_CODING");
        answerRepository.save(answer);

        // Update overall result
        updateAttemptFinalStatus(attemptId);
    }

    private void updateAttemptFinalStatus(UUID attemptId) {
        // TODO: Recalculate total score and check if all questions are graded
        // implementation to finalize if no more PENDING_SANDBOX or PENDING_AUTO
    }

    private BigDecimal autoEvaluate(Question question, StudentAnswer answer, BigDecimal maxMarks, BigDecimal negMark) {
        boolean isCorrect = false;
        try {
            JsonNode payload = question.getPayload();
            String rawAnswer = answer.getRawAnswer();

            isCorrect = switch (question.getType()) {
                case MCQ_SINGLE -> payload.get("correctOptionId").asText().equals(rawAnswer);
                case MCQ_MULTI -> {
                    Set<String> correctIds = new HashSet<>();
                    payload.get("correctOptionIds").forEach(id -> correctIds.add(id.asText()));
                    Set<String> studentIds = new HashSet<>(Arrays.asList(objectMapper.readValue(rawAnswer, String[].class)));
                    yield correctIds.equals(studentIds);
                }
                case TRUE_FALSE -> payload.get("correctAnswer").asBoolean() == Boolean.parseBoolean(rawAnswer);
                case FILL_BLANK -> true; // Simplified for now
                case MATCH -> true; // Simplified for now
                case SEQUENCE -> true; // Simplified for now
                default -> false;
            };
        } catch (Exception e) {
            log.error("Error evaluating question {}: {}", question.getId(), e.getMessage());
        }

        BigDecimal score = isCorrect ? maxMarks : negMark.negate();
        answer.setMarksObtained(score);
        answer.setEvaluationStatus(isCorrect ? "AUTO_GRADED_CORRECT" : "AUTO_GRADED_INCORRECT");
        answerRepository.save(answer);
        return score;
    }
}
