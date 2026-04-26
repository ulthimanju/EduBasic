package com.app.exam.dto;

import com.app.exam.domain.AttemptStatus;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AttemptResponse {
    private UUID id;
    private UUID examId;
    private UUID studentId;
    private AttemptStatus status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer version;
}
