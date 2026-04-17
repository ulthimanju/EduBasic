package com.app.exam.repository;

import com.app.exam.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByCourseIdAndDifficulty(UUID courseId, String difficulty);
    
    @Query(value = "SELECT * FROM questions q WHERE q.course_id = :courseId AND q.difficulty = :difficulty ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomByCourseIdAndDifficulty(UUID courseId, String difficulty, int limit);
}
