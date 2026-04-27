package com.nexora.courseservice.entity;

import com.nexora.courseservice.constants.ValidationMessages;

public record CompletionRules(
    boolean requireAllLessons,
    boolean requireAllExams,
    int minPassPercent
) {
    public CompletionRules {
        if (minPassPercent < 0 || minPassPercent > 100) {
            throw new IllegalArgumentException(ValidationMessages.INVALID_COMPLETION_RULES);
        }
    }

    public static CompletionRules defaultRules() {
        return new CompletionRules(true, true, 70);
    }
}
