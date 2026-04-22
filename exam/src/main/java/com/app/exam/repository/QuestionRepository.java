package com.app.exam.repository;

import com.app.exam.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByCourseIdAndDifficulty(UUID courseId, String difficulty);
    
    @Query(value = "SELECT * FROM questions q WHERE q.course_id = :courseId AND q.difficulty = :difficulty " +
            "AND q.id NOT IN (SELECT ua.question_id FROM user_answers ua WHERE ua.session_id = :sessionId) " +
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomByCourseIdAndDifficultyAndNotUsedInSession(UUID courseId, String difficulty, UUID sessionId, int limit);

    @Query(value = "SELECT * FROM questions q WHERE q.course_id = :courseId " +
            "AND q.id NOT IN (SELECT ua.question_id FROM user_answers ua WHERE ua.session_id = :sessionId) " +
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomByCourseIdAndNotUsedInSession(UUID courseId, UUID sessionId, int limit);
}
