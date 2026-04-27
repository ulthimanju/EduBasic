package com.nexora.courseservice.dto.request;

import com.nexora.courseservice.entity.CompletionRules;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCourseRequest {
    @NotBlank(message = "Course title is required")
    private String title;
    private String description;
    private String thumbnailUrl;
    private CompletionRules completionRules;
}
