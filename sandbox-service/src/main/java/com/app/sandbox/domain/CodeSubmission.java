package com.app.sandbox.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "code_submissions")
@Getter
@Setter
public class CodeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "attempt_id")
    private UUID attemptId;

    @Column(name = "question_id")
    private UUID questionId;

    private String language;

    @Column(columnDefinition = "TEXT")
    private String sourceCode;

    private String status; // QUEUED, RUNNING, COMPLETED, FAILED

    @Column(name = "overall_status")
    private String overallStatus; // PASSED, PARTIAL, FAILED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "test_case_results", columnDefinition = "JSONB")
    private Object testCaseResults;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    private OffsetDateTime completedAt;
}
