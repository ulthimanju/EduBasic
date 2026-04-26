package com.app.exam.repository;

import com.app.exam.domain.AttemptStatus;
import com.app.exam.domain.StudentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentAttemptRepository extends JpaRepository<StudentAttempt, UUID> {
    Optional<StudentAttempt> findByStudentIdAndExamIdAndStatus(UUID studentId, UUID examId, AttemptStatus status);
    long countByStudentIdAndExamId(UUID studentId, UUID examId);
}
