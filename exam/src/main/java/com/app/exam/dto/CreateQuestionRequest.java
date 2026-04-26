package com.app.exam.dto;

import com.app.exam.domain.Difficulty;
import com.app.exam.domain.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateQuestionRequest {
    @NotNull
    private QuestionType type;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private JsonNode payload;

    @NotNull
    private Difficulty difficulty;

    private List<String> tags;

    private BigDecimal defaultMarks;

    private BigDecimal defaultNegMark;

    private boolean isPublic;
}
