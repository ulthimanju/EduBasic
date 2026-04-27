package com.nexora.courseservice.repository;

import com.nexora.courseservice.entity.CourseCompletionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseCompletionLogRepository extends JpaRepository<CourseCompletionLog, UUID> {
    Optional<CourseCompletionLog> findByCourseIdAndStudentId(UUID courseId, UUID studentId);
    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);
}
