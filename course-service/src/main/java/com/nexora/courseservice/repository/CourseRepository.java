package com.nexora.courseservice.repository;

import com.nexora.courseservice.entity.Course;
import com.nexora.courseservice.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    
    @Query("SELECT c FROM Course c WHERE c.createdBy = :createdBy AND c.isDeleted = false")
    Page<Course> findByCreatedByAndIsDeletedFalse(@Param("createdBy") UUID createdBy, Pageable pageable);

    Page<Course> findByStatusAndIsDeletedFalse(CourseStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"modules", "modules.lessons", "courseExams"})
    Optional<Course> findByIdAndIsDeletedFalse(UUID id);

    @EntityGraph(attributePaths = {"modules", "modules.lessons", "courseExams"})
    Optional<Course> findByIdAndCreatedByAndIsDeletedFalse(UUID id, UUID createdBy);

    boolean existsByIdAndCreatedByAndIsDeletedFalse(UUID id, UUID createdBy);

    Page<Course> findByStatusAndIsDeletedFalseAndTitleContainingIgnoreCase(CourseStatus status, String keyword, Pageable pageable);

    @Query("SELECT c.id as id, c.title as title, c.description as description, c.thumbnailUrl as thumbnailUrl, " +
           "c.status as status, c.createdBy as createdBy, c.createdAt as createdAt, " +
           "(SELECT COUNT(m) FROM CourseModule m WHERE m.course = c AND m.isDeleted = false) as totalModules, " +
           "(SELECT COUNT(l) FROM Lesson l JOIN l.module m WHERE m.course = c AND l.isDeleted = false AND m.isDeleted = false) as totalLessons, " +
           "(SELECT COUNT(e) FROM CourseExam e WHERE e.course = c) as totalExams " +
           "FROM Course c WHERE c.status = :status AND c.isDeleted = false AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CourseProjection> findAllWithCounts(@Param("status") CourseStatus status, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c.id as id, c.title as title, c.description as description, c.thumbnailUrl as thumbnailUrl, " +
           "c.status as status, c.createdBy as createdBy, c.createdAt as createdAt, " +
           "(SELECT COUNT(m) FROM CourseModule m WHERE m.course = c AND m.isDeleted = false) as totalModules, " +
           "(SELECT COUNT(l) FROM Lesson l JOIN l.module m WHERE m.course = c AND l.isDeleted = false AND m.isDeleted = false) as totalLessons, " +
           "(SELECT COUNT(e) FROM CourseExam e WHERE e.course = c) as totalExams " +
           "FROM Course c WHERE c.createdBy = :createdBy AND c.isDeleted = false")
    Page<CourseProjection> findMyCoursesWithCounts(@Param("createdBy") UUID createdBy, Pageable pageable);

    interface CourseProjection {
        UUID getId();
        String getTitle();
        String getDescription();
        String getThumbnailUrl();
        CourseStatus getStatus();
        UUID getCreatedBy();
        java.time.LocalDateTime getCreatedAt();
        Integer getTotalModules();
        Integer getTotalLessons();
        Integer getTotalExams();
    }
}
