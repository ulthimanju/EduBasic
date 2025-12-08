package com.edubas.backend.repository;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import com.edubas.backend.entity.PracticeProblem;

@Repository
public interface PracticeProblemRepository extends Neo4jRepository<PracticeProblem, String> {

    @Query("MATCH (p:PracticeProblem)-[:FOR_LESSON]->(l:Lesson) " +
            "WHERE l.lessonId = $lessonId OR l.id = $lessonId " +
            "RETURN p ORDER BY p.createdAt DESC LIMIT 1")
    Optional<PracticeProblem> findLatestByLessonId(String lessonId);

    @Query("MATCH (u:User {username: $username})<-[:GENERATED_BY]-(p:PracticeProblem)-[:FOR_LESSON]->(l:Lesson) " +
            "WHERE l.lessonId = $lessonId OR l.id = $lessonId " +
            "RETURN p ORDER BY p.createdAt DESC LIMIT 1")
    Optional<PracticeProblem> findLatestByLessonIdAndUser(String lessonId, String username);
}
