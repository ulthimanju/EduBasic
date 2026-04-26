package com.app.exam.service;

import com.app.exam.domain.ProctoringLog;
import com.app.exam.domain.StudentAttempt;
import com.app.exam.dto.ViolationRequest;
import com.app.exam.dto.ViolationResponse;
import com.app.exam.repository.ProctoringLogRepository;
import com.app.exam.repository.StudentAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViolationService {

    private final ProctoringLogRepository logRepository;
    private final StudentAttemptRepository attemptRepository;
    private final AttemptService attemptService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String VIOLATION_PREFIX = "exam:violations:";

    @Transactional
    public ViolationResponse recordViolation(UUID attemptId, ViolationRequest request) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Persist to Postgres
        ProctoringLog proctoringLog = new ProctoringLog();
        proctoringLog.setAttempt(attempt);
        proctoringLog.setViolationType(request.getViolationType());
        proctoringLog.setMetadata(request.getMetadata());
        proctoringLog.setCapturedAt(request.getTimestamp());
        logRepository.save(proctoringLog);

        // Redis Counter
        String redisKey = VIOLATION_PREFIX + attemptId;
        Long violationCount = redisTemplate.opsForValue().increment(redisKey);
        if (violationCount == null) violationCount = 1L;

        int maxViolations = attempt.getExam().getMaxViolations();
        boolean autoSubmitted = false;

        if (violationCount >= maxViolations) {
            log.info("Attempt {} reached max violations ({}). Auto-submitting.", attemptId, violationCount);
            attemptService.submitAttemptInternal(attempt);
            autoSubmitted = true;
        }

        return new ViolationResponse(violationCount.intValue(), maxViolations, autoSubmitted);
    }
}
