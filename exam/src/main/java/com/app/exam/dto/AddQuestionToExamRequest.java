package com.app.exam.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
    @DecimalMin("0.0")
    private BigDecimal marks;

    @DecimalMin("0.0")
    private BigDecimal negMark;

    @NotNull
    @Min(0)
    private Integer orderIndex;

    private boolean isMandatory;
}
