package com.app.exam.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class QuestionResponse {
    private UUID sessionId;
    private UUID questionId;
    private String question;
    private List<String> options;
    private Integer index;
    private String difficulty;
    private Integer timeLimit; // in seconds
}
