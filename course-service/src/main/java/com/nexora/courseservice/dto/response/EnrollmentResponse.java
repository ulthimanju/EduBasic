package com.nexora.courseservice.dto.response;

import com.nexora.courseservice.entity.EnrollmentStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EnrollmentResponse {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}
