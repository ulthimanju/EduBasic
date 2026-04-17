package com.app.exam.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "exam_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ExamSession session;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    private String level;

    @Column(name = "raw_score")
    private Float rawScore;

    @Column(name = "normalized_score")
    private Float normalizedScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topics_strong", columnDefinition = "jsonb")
    private Map<String, Integer> topicsStrong;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topics_weak", columnDefinition = "jsonb")
    private Map<String, Integer> topicsWeak;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "difficulty_breakdown", columnDefinition = "jsonb")
    private Map<String, Integer> difficultyBreakdown;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
