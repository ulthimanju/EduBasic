package com.nexora.courseservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "course_exams")
@Getter
@Setter
public class CourseExam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(nullable = false)
    private String title;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "required_to_complete", nullable = false)
    private boolean requiredToComplete = true;

    @Column(name = "min_pass_percent")
    private Integer minPassPercent;
}
