package com.nexora.courseservice.dto.response;

import com.nexora.courseservice.entity.ProgressStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LessonProgressResponse {
    private UUID lessonId;
    private ProgressStatus status;
    private int progressPercent;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime completedAt;
}
