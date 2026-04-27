package com.nexora.courseservice.entity;

import com.nexora.courseservice.constants.ValidationMessages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompletionRulesTest {

    @Test
    void shouldCreateValidCompletionRules() {
        CompletionRules rules = new CompletionRules(true, true, 70);
        assertTrue(rules.requireAllLessons());
        assertTrue(rules.requireAllExams());
        assertEquals(70, rules.minPassPercent());
    }

    @Test
    void shouldThrowExceptionWhenMinPassPercentIsNegative() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            new CompletionRules(true, true, -1));
        assertEquals(ValidationMessages.INVALID_COMPLETION_RULES, exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenMinPassPercentIsOver100() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            new CompletionRules(true, true, 101));
        assertEquals(ValidationMessages.INVALID_COMPLETION_RULES, exception.getMessage());
    }
}
