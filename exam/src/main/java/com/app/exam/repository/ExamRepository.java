package com.app.exam.repository;

import com.app.exam.domain.Exam;
import com.app.exam.domain.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamRepository extends JpaRepository<Exam, UUID> {
    List<Exam> findAllByCreatedBy(UUID createdBy);
    List<Exam> findAllByCreatedByAndStatus(UUID createdBy, ExamStatus status);
    List<Exam> findAllByStatus(ExamStatus status);
}
