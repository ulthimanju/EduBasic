package com.nexora.courseservice.dto.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ModuleResponse {
    private UUID id;
    private String title;
    private String description;
    private int orderIndex;
    private List<LessonResponse> lessons;
}
