package com.app.exam.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ExamResultResponse {
    private UUID sessionId;
    private String level;
    private float rawScore;
    private float normalizedScore;
    private Map<String, Integer> topicsStrong;
    private Map<String, Integer> topicsWeak;
    private Map<String, Integer> difficultyBreakdown;
    private String status;
    private String terminationReason;
    private String warningMessage;
    private Integer violationCount;
}
