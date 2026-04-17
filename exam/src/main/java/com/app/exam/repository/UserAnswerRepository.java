package com.app.exam.repository;

import com.app.exam.domain.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, UUID> {
    List<UserAnswer> findBySessionId(UUID sessionId);
}
