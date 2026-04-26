package com.app.exam.dto;

import com.app.exam.domain.ViolationType;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class ViolationRequest {
    private ViolationType violationType;
    private OffsetDateTime timestamp;
    private Map<String, Object> metadata;
}
