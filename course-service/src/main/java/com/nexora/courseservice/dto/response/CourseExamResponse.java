package com.nexora.courseservice.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class CourseExamResponse {
    private UUID id;
    private UUID examId;
    private String title;
    private int orderIndex;
    private boolean requiredToComplete;
    private Integer minPassPercent;
}
