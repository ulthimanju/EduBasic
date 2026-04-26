package com.app.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ViolationResponse {
    private int violationCount;
    private int maxViolations;
    private boolean autoSubmitted;
}
