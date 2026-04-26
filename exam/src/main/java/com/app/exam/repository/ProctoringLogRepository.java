package com.app.exam.repository;

import com.app.exam.domain.ProctoringLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProctoringLogRepository extends JpaRepository<ProctoringLog, UUID> {
    Page<ProctoringLog> findAllByAttemptId(UUID attemptId, Pageable pageable);
}
