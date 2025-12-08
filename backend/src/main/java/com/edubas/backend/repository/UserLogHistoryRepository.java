package com.edubas.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.edubas.backend.entity.UserLogHistory;

@Repository
public interface UserLogHistoryRepository extends Neo4jRepository<UserLogHistory, String> {
    List<UserLogHistory> findByUserId(String userId);

    List<UserLogHistory> findByUserIdOrderByTimestampDesc(String userId);

    List<UserLogHistory> findByAction(String action);

    List<UserLogHistory> findByActionAndStatus(String action, String status);

    List<UserLogHistory> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<UserLogHistory> findByUserIdAndAction(String userId, String action);
}
