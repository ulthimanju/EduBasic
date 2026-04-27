package com.app.exam.dto;

import com.app.exam.domain.ViolationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class ViolationRequest {
    @NotNull
    private ViolationType violationType;

    @NotNull
    private OffsetDateTime timestamp;

    @Size(max = 20)
    private Map<String, Object> metadata;
}
