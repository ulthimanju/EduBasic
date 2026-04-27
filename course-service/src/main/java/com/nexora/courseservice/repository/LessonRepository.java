package com.nexora.courseservice.repository;

import com.nexora.courseservice.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByModuleIdAndIsDeletedFalseOrderByOrderIndex(UUID moduleId);
    Optional<Lesson> findByIdAndIsDeletedFalse(UUID id);
    boolean existsByModuleIdAndOrderIndexAndIsDeletedFalse(UUID moduleId, int orderIndex);
    long countByModuleIdAndIsDeletedFalse(UUID moduleId);

    @Query("""
        SELECT COUNT(l) FROM Lesson l
        JOIN CourseModule m ON l.module.id = m.id
        WHERE m.course.id = :courseId
        AND l.isDeleted = false
        AND m.isDeleted = false
        """)
    long countTotalLessonsForCourse(@Param("courseId") UUID courseId);
}
