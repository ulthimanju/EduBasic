package com.app.exam.repository;

import com.app.exam.domain.ExamQuestionMapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamQuestionMappingRepository extends JpaRepository<ExamQuestionMapping, UUID> {
    @EntityGraph(attributePaths = {"section", "question"})
    List<ExamQuestionMapping> findAllByExamIdOrderByOrderIndexAsc(UUID examId);

    @EntityGraph(attributePaths = {"question"})
    List<ExamQuestionMapping> findAllBySectionIdOrderByOrderIndexAsc(UUID sectionId);

    long countByExamId(UUID examId);
    long countBySectionId(UUID sectionId);

    boolean existsByExamIdAndQuestionId(UUID examId, UUID questionId);
}
