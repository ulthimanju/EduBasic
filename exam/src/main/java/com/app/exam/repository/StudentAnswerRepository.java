package com.app.exam.repository;

import com.app.exam.domain.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, UUID> {
    Optional<StudentAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
