package com.nexora.courseservice.event;

import java.util.UUID;

public record ExamCompletedEvent(
    UUID attemptId,
    UUID examId,
    UUID studentId,
    double score,
    boolean passed,
    String completedAt    // ISO-8601 string
) {}
