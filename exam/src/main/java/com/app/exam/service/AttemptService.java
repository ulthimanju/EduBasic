package com.app.exam.service;

import com.app.exam.domain.*;
import com.app.exam.dto.*;
import com.app.exam.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttemptService {

    private final StudentAttemptRepository attemptRepository;
    private final StudentAnswerRepository answerRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamQuestionMappingRepository mappingRepository;
    private final ExamSnapshotRepository snapshotRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_PREFIX = "exam:session:";

    @Transactional
    public AttemptResponse startAttempt(UUID studentId, UUID examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("Exam is not published");
        }

        // Check for existing IN_PROGRESS attempt
        Optional<StudentAttempt> existing = attemptRepository.findByStudentIdAndExamIdAndStatus(studentId, examId, AttemptStatus.IN_PROGRESS);
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        // Check attempt limits
        long attemptCount = attemptRepository.countByStudentIdAndExamId(studentId, examId);
        if (exam.getMaxAttempts() != null && attemptCount >= exam.getMaxAttempts()) {
            throw new RuntimeException("Max attempts reached for this exam");
        }

        StudentAttempt attempt = new StudentAttempt();
        attempt.setStudentId(studentId);
        attempt.setExam(exam);
        
        // Link to the latest snapshot
        ExamSnapshot snapshot = snapshotRepository.findByExamIdAndVersion(exam.getId(), exam.getCurrentVersion())
                .orElseThrow(() -> new RuntimeException("Published exam version not found"));
        attempt.setExamSnapshot(snapshot);

        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStartTime(OffsetDateTime.now());
        
        attempt = attemptRepository.save(attempt);

        // Initialize Redis Session
        initializeRedisSession(attempt, exam);

        return mapToResponse(attempt);
    }

    @Transactional
    public AttemptResponse syncAttempt(UUID studentId, UUID attemptId, SyncAttemptRequest request) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new RuntimeException("Unauthorized attempt access");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Attempt is already " + attempt.getStatus());
        }

        // Optimistic locking check via version
        if (!attempt.getVersion().equals(request.getVersion())) {
            throw new RuntimeException("Version mismatch. Please refresh your progress.");
        }

        // Update Redis cache (Source of truth for frequent autosaves)
        String redisKey = REDIS_PREFIX + attemptId;
        redisTemplate.opsForHash().put(redisKey, "answers", request.getAnswers());
        
        // Persist to Postgres on every sync to prevent data loss
        updateAnswers(attempt, request.getAnswers());

        // Increment version in Postgres (heartbeat and lock)
        attempt.setVersion(attempt.getVersion() + 1);
        return mapToResponse(attemptRepository.save(attempt));
    }

    @Transactional
    public void submitAttempt(UUID studentId, UUID attemptId) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new RuntimeException("Unauthorized attempt access");
        }

        // Final flush from Redis to Postgres on submit
        String redisKey = REDIS_PREFIX + attemptId;
        Object rawAnswers = redisTemplate.opsForHash().get(redisKey, "answers");
        if (rawAnswers instanceof Map<?, ?> rawMap) {
            Map<UUID, String> latestAnswers = rawMap.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(Collectors.toMap(
                    e -> UUID.fromString(e.getKey().toString()),
                    e -> e.getValue().toString()
                ));
            updateAnswers(attempt, latestAnswers);
        }

        submitAttemptInternal(attempt);
    }

    @Transactional
    public void submitAttemptInternal(StudentAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return; // Already submitted or evaluated
        }

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setEndTime(OffsetDateTime.now());
        attemptRepository.save(attempt);

        // Clear Redis Session
        redisTemplate.delete(REDIS_PREFIX + attempt.getId());

        // Publish to Kafka
        SubmitAttemptEvent event = new SubmitAttemptEvent(attempt.getId(), attempt.getStudentId(), attempt.getExam().getId());
        kafkaTemplate.send("exam-submitted", attempt.getId().toString(), event);
        log.info("Published submission event for attempt: {}", attempt.getId());
    }

    @Transactional
    public void autoSubmitAttempt(UUID attemptId) {
        attemptRepository.findById(attemptId).ifPresent(attempt -> {
            // Final flush from Redis to Postgres on auto-submit
            String redisKey = REDIS_PREFIX + attemptId;
            Object rawAnswers = redisTemplate.opsForHash().get(redisKey, "answers");
            if (rawAnswers instanceof Map<?, ?> rawMap) {
                Map<UUID, String> latestAnswers = rawMap.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    .collect(Collectors.toMap(
                        e -> UUID.fromString(e.getKey().toString()),
                        e -> e.getValue().toString()
                    ));
                updateAnswers(attempt, latestAnswers);
            }
            submitAttemptInternal(attempt);
        });
    }

    @Transactional(readOnly = true)
    public AttemptResponse getAttempt(UUID studentId, UUID attemptId) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new RuntimeException("Unauthorized attempt access");
        }

        return mapToResponse(attempt);
    }

    private void initializeRedisSession(StudentAttempt attempt, Exam exam) {
        String redisKey = REDIS_PREFIX + attempt.getId();
        Map<String, Object> sessionData = Map.of(
                "studentId", attempt.getStudentId().toString(),
                "examId", exam.getId().toString(),
                "startTime", attempt.getStartTime().toString(),
                "version", 0,
                "answers", new HashMap<UUID, String>()
        );
        redisTemplate.opsForHash().putAll(redisKey, sessionData);
        
        // TTL: exam duration + 15-minute grace period
        if (exam.getTimeLimitMins() != null) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(exam.getTimeLimitMins() + 15));
        }
    }

    private void updateAnswers(StudentAttempt attempt, Map<UUID, String> answers) {
        if (answers == null || answers.isEmpty()) {
            return;
        }

        UUID examId = attempt.getExam().getId();
        List<StudentAnswer> existingAnswers = answerRepository.findAllByAttemptId(attempt.getId());
        Map<UUID, StudentAnswer> existingMap = existingAnswers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

        List<StudentAnswer> toSave = new ArrayList<>();

        for (Map.Entry<UUID, String> entry : answers.entrySet()) {
            UUID questionId = entry.getKey();
            String rawAnswer = entry.getValue();

            // 1. Security Check: Ensure question belongs to the attempt's exam
            if (!mappingRepository.existsByExamIdAndQuestionId(examId, questionId)) {
                log.warn("Security alert: Attempt to save answer for question {} not in exam {} by student {}", 
                        questionId, examId, attempt.getStudentId());
                continue;
            }

            // 2. Abuse Protection: Limit answer size (e.g., 50KB)
            if (rawAnswer != null && rawAnswer.length() > 50000) {
                log.warn("Abuse protection: Answer for question {} too large ({} chars) for attempt {}", 
                        questionId, rawAnswer.length(), attempt.getId());
                continue;
            }

            StudentAnswer answer = existingMap.get(questionId);
            if (answer != null) {
                if (rawAnswer != null && !rawAnswer.equals(answer.getRawAnswer())) {
                    answer.setRawAnswer(rawAnswer);
                    answer.setEvaluationStatus("PENDING");
                    toSave.add(answer);
                }
            } else {
                StudentAnswer a = new StudentAnswer();
                a.setAttempt(attempt);
                a.setQuestion(questionRepository.getReferenceById(questionId));
                a.setRawAnswer(rawAnswer);
                a.setEvaluationStatus("PENDING");
                toSave.add(a);
            }
        }

        if (!toSave.isEmpty()) {
            answerRepository.saveAll(toSave);
        }
    }

    private AttemptResponse mapToResponse(StudentAttempt attempt) {
        AttemptResponse response = new AttemptResponse();
        response.setId(attempt.getId());
        response.setExamId(attempt.getExam().getId());
        response.setStudentId(attempt.getStudentId());
        response.setStatus(attempt.getStatus());
        response.setStartTime(attempt.getStartTime());
        response.setEndTime(attempt.getEndTime());
        response.setVersion(attempt.getVersion());
        return response;
    }
}
