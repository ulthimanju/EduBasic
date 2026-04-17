package com.app.exam.repository;

import com.app.exam.domain.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ResultRepository extends JpaRepository<ExamResult, UUID> {
    Optional<ExamResult> findBySessionId(UUID sessionId);
}
