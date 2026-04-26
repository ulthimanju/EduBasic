package com.app.exam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "proctoring_logs")
@Getter
@Setter
public class ProctoringLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private StudentAttempt attempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type")
    private ViolationType violationType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    @Column(name = "captured_at")
    private OffsetDateTime capturedAt = OffsetDateTime.now();
}
