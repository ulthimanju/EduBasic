package com.nexora.courseservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_completion_log")
@Getter
@Setter
public class CourseCompletionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "trigger_type", nullable = false)
    private String triggerType;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String snapshot;

    @PrePersist
    protected void onCreate() {
        completedAt = LocalDateTime.now();
    }
}
