package com.nexora.courseservice.dto.response;

import com.nexora.courseservice.entity.CompletionRules;
import com.nexora.courseservice.entity.CourseStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CourseResponse {
    private UUID id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private CourseStatus status;
    private CompletionRules completionRules;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ModuleResponse> modules;
    private List<CourseExamResponse> exams;
}
