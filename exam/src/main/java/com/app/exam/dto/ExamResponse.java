package com.app.exam.dto;

import com.app.exam.domain.ExamStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ExamResponse {
    private UUID id;
    private UUID createdBy;
    private String title;
    private String description;
    private boolean hasSections;
    private Integer timeLimitMins;
    private boolean shuffleQuestions;
    private boolean shuffleOptions;
    private boolean allowBacktrack;
    private Integer maxAttempts;
    private BigDecimal passMarks;
    private boolean negativeMarking;
    private ExamStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<ExamSectionResponse> sections;
    private List<ExamQuestionMappingResponse> questions; // For flat exams
}
