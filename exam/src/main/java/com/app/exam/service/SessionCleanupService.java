package com.app.exam.service;

import com.app.exam.LogMessages;
import com.app.exam.domain.ExamSession;
import com.app.exam.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupService {
    private final ExamSessionRepository sessionRepository;

    private static final int TIMEOUT_MINUTES = 30;

    /**
     * Run every 5 minutes to find and mark abandoned sessions.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupAbandonedSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<ExamSession> staleSessions = sessionRepository.findByStatusAndLastActivityAtBefore(
                ExamSession.Status.ACTIVE, threshold);

        if (!staleSessions.isEmpty()) {
            log.info(LogMessages.FOUND_STALE_SESSIONS_ABANDONED, staleSessions.size());
            for (ExamSession session : staleSessions) {
                session.setStatus(ExamSession.Status.ABANDONED);
                session.setTerminationReason("Session timed out after " + TIMEOUT_MINUTES + " minutes of inactivity.");
                session.setCompletedAt(LocalDateTime.now());
            }
            sessionRepository.saveAll(staleSessions);
        }
    }
}
