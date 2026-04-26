package com.app.exam.service;

import com.app.exam.domain.AttemptStatus;
import com.app.exam.domain.ProctoringLog;
import com.app.exam.domain.StudentAttempt;
import com.app.exam.repository.ProctoringLogRepository;
import com.app.exam.repository.StudentAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProctoringService {

    private final ProctoringLogRepository logRepository;
    private final StudentAttemptRepository attemptRepository;

    @Transactional
    public void logEvent(UUID attemptId, String eventType, Map<String, Object> eventData) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            log.warn("Ignoring proctoring event for attempt {} which is in status {}", attemptId, attempt.getStatus());
            return;
        }

        ProctoringLog proctoringLog = new ProctoringLog();
        proctoringLog.setAttempt(attempt);
        proctoringLog.setEventType(eventType);
        proctoringLog.setEventData(eventData);
        proctoringLog.setCapturedAt(OffsetDateTime.now());

        logRepository.save(proctoringLog);
        log.info("Proctoring event {} logged for attempt {}", eventType, attemptId);
    }

    public List<ProctoringLog> getLogs(UUID attemptId) {
        return logRepository.findAllByAttemptIdOrderByCapturedAtDesc(attemptId);
    }
}
