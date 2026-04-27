package com.nexora.courseservice.dto.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CompletionStatusResponse {
    private UUID courseId;
    private boolean isCompleted;
    private List<String> remainingLessons;
    private List<UUID> remainingExamIds;
    private int currentAvgExamScore;
    private int requiredPassPercent;
}
