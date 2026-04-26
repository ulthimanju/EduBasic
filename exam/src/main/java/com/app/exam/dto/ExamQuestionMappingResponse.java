package com.app.exam.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ExamQuestionMappingResponse {
    private UUID id;
    private UUID questionId;
    private QuestionResponse question;
    private Integer orderIndex;
    private BigDecimal marks;
    private BigDecimal negMark;
    private boolean isMandatory;
}
