package com.nexora.courseservice.dto.response;

import com.nexora.courseservice.entity.CompletionRules;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CourseOutlineResponse {
    private UUID id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private CompletionRules completionRules;
    private List<ModuleOutlineResponse> modules;
    private List<CourseExamResponse> exams;
}
