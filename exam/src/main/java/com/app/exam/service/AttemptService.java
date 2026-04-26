package com.app.exam.service;

import com.app.exam.domain.*;
import com.app.exam.dto.*;
import com.app.exam.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttemptService {

    private final StudentAttemptRepository attemptRepository;
    private final StudentAnswerRepository answerRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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

        // Update Postgres with answers
        updateAnswers(attempt, request.getAnswers());

        // Update Redis cache
        String redisKey = REDIS_PREFIX + attemptId;
        redisTemplate.opsForHash().put(redisKey, "answers", request.getAnswers());
        redisTemplate.opsForHash().put(redisKey, "version", attempt.getVersion() + 1);

        // attemptRepository.save will increment version due to @Version
        return mapToResponse(attemptRepository.save(attempt));
    }

    @Transactional
    public void submitAttempt(UUID studentId, UUID attemptId) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new RuntimeException("Unauthorized attempt access");
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
        attemptRepository.findById(attemptId).ifPresent(this::submitAttemptInternal);
    }

    private void initializeRedisSession(StudentAttempt attempt, Exam exam) {
        String redisKey = REDIS_PREFIX + attempt.getId();
        Map<String, Object> sessionData = Map.of(
                "studentId", attempt.getStudentId().toString(),
                "examId", exam.getId().toString(),
                "startTime", attempt.getStartTime().toString(),
                "version", 0
        );
        redisTemplate.opsForHash().putAll(redisKey, sessionData);
        
        // TTL: exam duration + 15-minute grace period
        if (exam.getTimeLimitMins() != null) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(exam.getTimeLimitMins() + 15));
        }
    }

    private void updateAnswers(StudentAttempt attempt, Map<UUID, String> answers) {
        for (Map.Entry<UUID, String> entry : answers.entrySet()) {
            UUID questionId = entry.getKey();
            String rawAnswer = entry.getValue();

            StudentAnswer answer = answerRepository.findByAttemptIdAndQuestionId(attempt.getId(), questionId)
                    .orElseGet(() -> {
                        StudentAnswer a = new StudentAnswer();
                        a.setAttempt(attempt);
                        a.setQuestion(questionRepository.getReferenceById(questionId));
                        return a;
                    });
            
            answer.setRawAnswer(rawAnswer);
            answer.setEvaluationStatus("PENDING");
            answerRepository.save(answer);
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
