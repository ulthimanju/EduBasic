package com.nexora.courseservice.dto.response;

import java.time.LocalDateTime;

public record ExamScoreCache(double score, boolean passed, LocalDateTime completedAt) {}
