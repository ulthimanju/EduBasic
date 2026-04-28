package com.app.exam.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateExamRequest {
    @NotBlank
    private String title;

    private String description;

    private boolean hasSections;

    @Min(0)
    private Integer timeLimitMins;

    private boolean shuffleQuestions = true;

    private boolean shuffleOptions = true;

    private boolean allowBacktrack = true;

    @Min(1)
    private Integer maxAttempts = 1;

    @DecimalMin("0.0")
    private BigDecimal passMarks;

    private boolean negativeMarking;
}
