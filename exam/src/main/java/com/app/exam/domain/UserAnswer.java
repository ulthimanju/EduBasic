package com.app.exam.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ExamSession session;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "selected_option", length = 255)
    private String selectedOption;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "time_taken")
    private Integer timeTaken;

    @Column(name = "answered_at")
    @Builder.Default
    private LocalDateTime answeredAt = LocalDateTime.now();
}
