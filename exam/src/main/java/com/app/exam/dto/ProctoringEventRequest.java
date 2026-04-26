package com.app.exam.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ProctoringEventRequest {
    private String eventType;
    private Map<String, Object> eventData;
}
