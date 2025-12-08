package com.edubas.backend.repository;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import com.edubas.backend.entity.Lesson;

@Repository
public interface LessonRepository extends Neo4jRepository<Lesson, String> {

    @Query("MATCH (l:Lesson) WHERE l.lessonId = $lessonId OR l.id = $lessonId RETURN l LIMIT 1")
    Optional<Lesson> findByLessonIdOrId(String lessonId);
}
