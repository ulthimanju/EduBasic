package com.app.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class ProctoringEventRequest {
    @NotBlank
    private String eventType;

    @Size(max = 20)
    private Map<String, Object> eventData;
}
