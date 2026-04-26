package com.app.exam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateExamRequest {
    @NotBlank
    private String title;

    private String description;

    private boolean hasSections;

    private Integer timeLimitMins;

    private boolean shuffleQuestions = true;

    private boolean shuffleOptions = true;

    private boolean allowBacktrack = true;

    private Integer maxAttempts = 1;

    private BigDecimal passMarks;

    private boolean negativeMarking;
}
