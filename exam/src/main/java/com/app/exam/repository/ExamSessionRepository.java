package com.app.exam.repository;

import com.app.exam.domain.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {
    List<ExamSession> findByStatusAndLastActivityAtBefore(ExamSession.Status status, LocalDateTime threshold);
}
