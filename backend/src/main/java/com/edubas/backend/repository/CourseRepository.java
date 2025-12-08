package com.edubas.backend.repository;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.edubas.backend.entity.Course;

@Repository
public interface CourseRepository extends Neo4jRepository<Course, String> {
    Optional<Course> findByCourseId(String courseId);

    boolean existsByCourseId(String courseId);
}
