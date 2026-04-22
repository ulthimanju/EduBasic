package com.app.exam.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnswerResponse {
    private boolean correct;
    private String correctAnswer;
    private String explanation;
    private boolean sessionComplete;
    private String warningMessage;
}
