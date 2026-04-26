package com.app.exam.service;

import com.app.exam.domain.*;
import com.app.exam.dto.SubmitAttemptEvent;
import com.app.exam.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final CodingSandboxService codingSandboxService;

    @Transactional
    public void evaluateAttempt(UUID attemptId) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found: " + attemptId));

        log.info("Starting auto-evaluation for attempt: {}", attemptId);

        List<ExamQuestionMapping> mappings = mappingRepository.findAllByExamIdOrderByOrderIndexAsc(attempt.getExam().getId());
        List<StudentAnswer> answers = answerRepository.findAll(); // Optimization: filter by attemptId in repo

        BigDecimal totalScore = BigDecimal.ZERO;
        boolean needsManualEvaluation = false;

        EvaluationResult result = resultRepository.findByAttemptId(attemptId)
                .orElseGet(() -> {
                    EvaluationResult r = new EvaluationResult();
                    r.setAttempt(attempt);
                    r.setStatus(EvaluationStatus.PENDING_AUTO);
                    return r;
                });

        Map<String, Object> details = new HashMap<>();

        for (ExamQuestionMapping mapping : mappings) {
            Question question = mapping.getQuestion();
            Optional<StudentAnswer> answerOpt = answerRepository.findByAttemptIdAndQuestionId(attemptId, question.getId());
            
            BigDecimal marksForQuestion = BigDecimal.ZERO;

            if (isAutoEvaluable(question.getType())) {
                if (answerOpt.isPresent()) {
                    marksForQuestion = autoEvaluate(question, answerOpt.get(), mapping.getMarks(), attempt.getExam().isNegativeMarking() ? mapping.getNegMark() : BigDecimal.ZERO);
                }
            } else {
                needsManualEvaluation = true;
                if (answerOpt.isPresent()) {
                    StudentAnswer answer = answerOpt.get();
                    answer.setEvaluationStatus("PENDING_MANUAL");
                    answerRepository.save(answer);
                }
            }
            totalScore = totalScore.add(marksForQuestion);
        }

        result.setTotalScore(totalScore);
        result.setStatus(needsManualEvaluation ? EvaluationStatus.PENDING_MANUAL : EvaluationStatus.COMPLETED);
        result.setEvaluatedAt(OffsetDateTime.now());
        result.setResultJson(details);
        resultRepository.save(result);

        attempt.setScore(totalScore);
        if (!needsManualEvaluation) {
            attempt.setStatus(AttemptStatus.EVALUATED);
        }
        attemptRepository.save(attempt);

        // Notify via Kafka
        if (needsManualEvaluation) {
            kafkaTemplate.send("evaluation-needs-manual", attemptId.toString(), new SubmitAttemptEvent(attemptId, attempt.getStudentId(), attempt.getExam().getId()));
        } else {
            kafkaTemplate.send("evaluation-completed", attemptId.toString(), new SubmitAttemptEvent(attemptId, attempt.getStudentId(), attempt.getExam().getId()));
        }

        log.info("Auto-evaluation finished for attempt: {}. Needs manual: {}", attemptId, needsManualEvaluation);
    }

    private boolean isAutoEvaluable(QuestionType type) {
        return switch (type) {
            case MCQ_SINGLE, MCQ_MULTI, TRUE_FALSE, FILL_BLANK, MATCH, SEQUENCE, CODING -> true;
            case SUBJECTIVE -> false;
        };
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
                case FILL_BLANK -> {
                    // Simple exact match for all blanks
                    JsonNode blanks = payload.get("blanks");
                    Map<String, String> studentAnswers = objectMapper.readValue(rawAnswer, Map.class);
                    boolean allMatch = true;
                    for (JsonNode blank : blanks) {
                        String id = blank.get("id").asText();
                        String studentAns = studentAnswers.get(id);
                        boolean matchFound = false;
                        for (JsonNode accepted : blank.get("acceptedAnswers")) {
                            if (accepted.asText().equalsIgnoreCase(studentAns != null ? studentAns.trim() : "")) {
                                matchFound = true;
                                break;
                            }
                        }
                        if (!matchFound) {
                            allMatch = false;
                            break;
                        }
                    }
                    yield allMatch;
                }
                case MATCH -> {
                    JsonNode correctPairs = payload.get("correctPairs");
                    Map<String, String> studentPairs = objectMapper.readValue(rawAnswer, Map.class);
                    boolean allMatch = true;
                    for (JsonNode pair : correctPairs) {
                        if (!pair.get("rightId").asText().equals(studentPairs.get(pair.get("leftId").asText()))) {
                            allMatch = false;
                            break;
                        }
                    }
                    yield allMatch;
                }
                case SEQUENCE -> {
                    List<String> correctOrder = objectMapper.convertValue(payload.get("correctOrder"), List.class);
                    List<String> studentOrder = objectMapper.readValue(rawAnswer, List.class);
                    yield correctOrder.equals(studentOrder);
                }
                case CODING -> codingSandboxService.evaluate(rawAnswer, payload.get("testCases"));
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
