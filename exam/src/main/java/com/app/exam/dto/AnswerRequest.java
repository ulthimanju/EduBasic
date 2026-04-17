package com.app.exam.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AnswerRequest {
    private UUID questionId;
    private String selectedOption;
    private Integer timeTaken;
}
