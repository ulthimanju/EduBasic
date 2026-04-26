package com.app.exam.repository;

import com.app.exam.domain.Exam;
import com.app.exam.domain.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExamRepository extends JpaRepository<Exam, UUID> {
    Page<Exam> findAllByCreatedBy(UUID createdBy, Pageable pageable);
    Page<Exam> findAllByCreatedByAndStatus(UUID createdBy, ExamStatus status, Pageable pageable);
    Page<Exam> findAllByStatus(ExamStatus status, Pageable pageable);
}
