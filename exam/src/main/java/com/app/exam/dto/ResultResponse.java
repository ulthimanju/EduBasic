package com.app.exam.dto;

import com.app.exam.domain.EvaluationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class ResultResponse {
    private UUID id;
    private UUID attemptId;
    private EvaluationStatus status;
    private BigDecimal totalScore;
    private Map<String, Object> resultJson;
    private OffsetDateTime evaluatedAt;
}
