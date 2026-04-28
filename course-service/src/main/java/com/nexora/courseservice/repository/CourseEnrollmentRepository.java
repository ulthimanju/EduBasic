package com.nexora.courseservice.repository;

import com.nexora.courseservice.entity.CourseEnrollment;
import com.nexora.courseservice.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
    Optional<CourseEnrollment> findByCourseIdAndStudentId(UUID courseId, UUID studentId);
    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);
    boolean existsByCourseIdAndStudentIdAndStatusNot(UUID courseId, UUID studentId, EnrollmentStatus status);
    Page<CourseEnrollment> findByStudentIdAndStatusNot(UUID studentId, EnrollmentStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
        SELECT 
            ce.id as id,
            ce.courseId as courseId,
            c.title as courseTitle,
            c.thumbnailUrl as thumbnailUrl,
            ce.status as status,
            ce.enrolledAt as enrolledAt,
            ce.completedAt as completedAt,
            (SELECT COUNT(l) FROM Lesson l 
             JOIN CourseModule m ON l.module.id = m.id 
             WHERE m.course.id = ce.courseId AND l.isDeleted = false AND m.isDeleted = false) as totalLessons,
            (SELECT COUNT(lp) FROM LessonProgress lp 
             JOIN Lesson l2 ON lp.lessonId = l2.id 
             JOIN CourseModule m2 ON l2.module.id = m2.id 
             WHERE m2.course.id = ce.courseId AND lp.studentId = :studentId AND lp.status = 'COMPLETED') as completedLessons,
            (SELECT COUNT(cx) FROM CourseExam cx 
             WHERE cx.course.id = ce.courseId AND cx.requiredToComplete = true) as totalRequiredExams
        FROM CourseEnrollment ce
        JOIN Course c ON ce.courseId = c.id
        WHERE ce.studentId = :studentId
        AND ce.status <> :status
        AND c.isDeleted = false
    """)
    Page<EnrollmentSummaryProjection> findSummariesByStudentId(@org.springframework.data.repository.query.Param("studentId") UUID studentId, @org.springframework.data.repository.query.Param("status") EnrollmentStatus status, Pageable pageable);

    interface EnrollmentSummaryProjection {
        UUID getId();
        UUID getCourseId();
        String getCourseTitle();
        String getThumbnailUrl();
        EnrollmentStatus getStatus();
        LocalDateTime getEnrolledAt();
        LocalDateTime getCompletedAt();
        long getTotalLessons();
        long getCompletedLessons();
        long getTotalRequiredExams();
    }
}
