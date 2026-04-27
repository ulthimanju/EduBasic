package com.nexora.courseservice.dto.request;

import com.nexora.courseservice.entity.CompletionRules;
import lombok.Data;

@Data
public class UpdateCourseRequest {
    private String title;
    private String description;
    private String thumbnailUrl;
    private CompletionRules completionRules;
}
