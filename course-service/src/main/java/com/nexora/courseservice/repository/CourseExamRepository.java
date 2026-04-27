package com.nexora.courseservice.repository;

import com.nexora.courseservice.entity.CourseExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseExamRepository extends JpaRepository<CourseExam, UUID> {
    List<CourseExam> findByCourseIdOrderByOrderIndex(UUID courseId);
    Optional<CourseExam> findByCourseIdAndExamId(UUID courseId, UUID examId);
    boolean existsByCourseIdAndExamId(UUID courseId, UUID examId);
    void deleteByCourseIdAndExamId(UUID courseId, UUID examId);
}
