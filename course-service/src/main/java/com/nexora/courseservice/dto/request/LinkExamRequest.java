package com.nexora.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class LinkExamRequest {
    @NotNull(message = "Exam ID is required")
    private UUID examId;
    @NotBlank(message = "Exam title is required")
    private String title;
    @NotNull(message = "Order index is required")
    private Integer orderIndex;
    private boolean requiredToComplete = true;
    private Integer minPassPercent;
}
