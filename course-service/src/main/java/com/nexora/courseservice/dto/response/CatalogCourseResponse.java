package com.nexora.courseservice.dto.response;

import com.nexora.courseservice.entity.CourseStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CatalogCourseResponse {
    private UUID id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private int totalModules;
    private int totalLessons;
    private int totalExams;
    private CourseStatus status;
    private UUID createdBy;
    private LocalDateTime createdAt;
}
