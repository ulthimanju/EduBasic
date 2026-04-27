package com.nexora.courseservice.repository;

import com.nexora.courseservice.entity.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {
    List<CourseModule> findByCourseIdAndIsDeletedFalseOrderByOrderIndex(UUID courseId);
    Optional<CourseModule> findByIdAndIsDeletedFalse(UUID id);
    boolean existsByCourseIdAndOrderIndexAndIsDeletedFalse(UUID courseId, int orderIndex);
}
