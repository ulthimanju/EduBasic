package com.nexora.courseservice.dto.response;

import com.nexora.courseservice.entity.EnrollmentStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MyCourseSummaryResponse {
    private UUID courseId;
    private String courseTitle;
    private String thumbnailUrl;
    private EnrollmentStatus enrollmentStatus;
    private int totalLessons;
    private int completedLessons;
    private int totalRequiredExams;
    private int passedExams;
    private int overallProgressPercent;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}
