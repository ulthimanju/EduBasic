package com.app.exam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "exams")
@Getter
@Setter
@SQLDelete(sql = "UPDATE exams SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Exam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "has_sections")
    private boolean hasSections = false;

    @Column(name = "time_limit_mins")
    private Integer timeLimitMins;

    @Column(name = "shuffle_questions")
    private boolean shuffleQuestions = true;

    @Column(name = "shuffle_options")
    private boolean shuffleOptions = true;

    @Column(name = "allow_backtrack")
    private boolean allowBacktrack = true;

    @Column(name = "max_attempts")
    private Integer maxAttempts = 1;

    @Column(name = "pass_marks")
    private BigDecimal passMarks;

    @Column(name = "negative_marking")
    private boolean negativeMarking = false;

    @Enumerated(EnumType.STRING)
    private ExamStatus status = ExamStatus.DRAFT;
}
