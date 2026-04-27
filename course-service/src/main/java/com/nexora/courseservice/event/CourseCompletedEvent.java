package com.nexora.courseservice.event;

import java.util.UUID;

public record CourseCompletedEvent(
    UUID courseId,
    UUID studentId,
    String completedAt,   // ISO-8601 string
    String triggerType    // always "AUTO" for Kafka-triggered completions
) {}
