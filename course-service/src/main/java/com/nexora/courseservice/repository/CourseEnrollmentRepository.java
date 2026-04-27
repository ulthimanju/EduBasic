package com.nexora.courseservice.repository;

import com.nexora.courseservice.entity.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
    Optional<CourseEnrollment> findByCourseIdAndStudentId(UUID courseId, UUID studentId);
    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);
}
