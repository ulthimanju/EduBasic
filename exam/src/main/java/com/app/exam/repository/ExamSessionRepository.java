package com.app.exam.repository;

import com.app.exam.domain.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {
}
