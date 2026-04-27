package com.nexora.courseservice.dto.response;

import com.nexora.courseservice.entity.CourseStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CourseSummaryResponse {
    private UUID id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private CourseStatus status;
    private int totalModules;
    private int totalLessons;
    private LocalDateTime createdAt;
}
