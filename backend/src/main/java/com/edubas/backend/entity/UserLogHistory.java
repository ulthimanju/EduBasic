package com.edubas.backend.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Node("UserLogHistory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLogHistory {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    private String userId;
    private String username;
    private String email;
    private String action; // LOGIN, LOGOUT, FAILED_LOGIN, REGISTER, PASSWORD_CHANGE, etc.
    private String status; // SUCCESS, FAILURE
    private String ipAddress;
    private String userAgent;
    private String failureReason; // null if successful, error message if failed
    private LocalDateTime timestamp;
    private String sessionId;

    public UserLogHistory(String userId, String username, String email, String action,
            String status, String ipAddress, String userAgent, LocalDateTime timestamp) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.action = action;
        this.status = status;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = timestamp;
    }

    public UserLogHistory(String userId, String username, String email, String action,
            String status, String ipAddress, String userAgent,
            String failureReason, LocalDateTime timestamp) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.action = action;
        this.status = status;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.failureReason = failureReason;
        this.timestamp = timestamp;
    }
}
