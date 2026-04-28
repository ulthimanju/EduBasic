package com.nexora.courseservice.repository;

import com.nexora.courseservice.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {
    Optional<LessonProgress> findByLessonIdAndStudentId(UUID lessonId, UUID studentId);

    @Query("""
        SELECT lp FROM LessonProgress lp
        JOIN Lesson l ON lp.lessonId = l.id
        JOIN CourseModule m ON l.module.id = m.id
        WHERE m.course.id = :courseId
        AND lp.studentId = :studentId
        AND l.isDeleted = false
        AND m.isDeleted = false
        """)
    List<LessonProgress> findAllByStudentIdAndCourseId(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);

    @Query("""
        SELECT COUNT(lp) FROM LessonProgress lp
        JOIN Lesson l ON lp.lessonId = l.id
        JOIN CourseModule m ON l.module.id = m.id
        WHERE m.course.id = :courseId
        AND lp.studentId = :studentId
        AND lp.status = 'COMPLETED'
        AND l.isDeleted = false
        AND m.isDeleted = false
        """)
    long countCompletedLessonsForCourse(@Param("courseId") UUID courseId, @Param("studentId") UUID studentId);
}
