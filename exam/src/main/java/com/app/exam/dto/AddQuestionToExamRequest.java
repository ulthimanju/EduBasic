package com.app.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AddQuestionToExamRequest {
    @NotNull
    private UUID questionId;

    private UUID sectionId; // Nullable for flat exams

    @NotNull
    private BigDecimal marks;

    private BigDecimal negMark;

    @NotNull
    private Integer orderIndex;

    private boolean isMandatory;
}
