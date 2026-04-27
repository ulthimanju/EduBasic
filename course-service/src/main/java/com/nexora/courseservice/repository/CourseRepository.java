package com.nexora.courseservice.repository;

import com.nexora.courseservice.entity.Course;
import com.nexora.courseservice.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    Page<Course> findByCreatedByAndIsDeletedFalse(UUID createdBy, Pageable pageable);
    Page<Course> findByStatusAndIsDeletedFalse(CourseStatus status, Pageable pageable);
    Optional<Course> findByIdAndIsDeletedFalse(UUID id);
    Optional<Course> findByIdAndCreatedByAndIsDeletedFalse(UUID id, UUID createdBy);
}
