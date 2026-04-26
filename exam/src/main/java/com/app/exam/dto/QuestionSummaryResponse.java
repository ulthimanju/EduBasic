package com.app.exam.dto;

import com.app.exam.domain.Difficulty;
import com.app.exam.domain.QuestionType;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class QuestionSummaryResponse {
    private UUID id;
    private UUID createdBy;
    private QuestionType type;
    private String title;
    private Difficulty difficulty;
    private List<String> tags;
}
