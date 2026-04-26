package com.app.exam.repository;

import com.app.exam.domain.ExamSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamSectionRepository extends JpaRepository<ExamSection, UUID> {
    List<ExamSection> findAllByExamIdOrderByOrderIndexAsc(UUID examId);
}
