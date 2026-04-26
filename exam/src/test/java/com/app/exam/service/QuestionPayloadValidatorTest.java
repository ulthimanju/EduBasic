package com.app.exam.service;

import com.app.exam.domain.QuestionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionPayloadValidatorTest {

    private QuestionPayloadValidator validator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        validator = new QuestionPayloadValidator();
        mapper = new ObjectMapper();
    }

    @Test
    void validateMcqSingle_Success() {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode options = payload.putArray("options");
        options.addObject().put("id", "1").put("text", "Opt 1");
        options.addObject().put("id", "2").put("text", "Opt 2");
        payload.put("correctOptionId", "1");

        assertDoesNotThrow(() -> validator.validate(QuestionType.MCQ_SINGLE, payload));
    }

    @Test
    void validateMcqSingle_Fail_NoCorrectId() {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode options = payload.putArray("options");
        options.addObject().put("id", "1").put("text", "Opt 1");
        options.addObject().put("id", "2").put("text", "Opt 2");

        assertThrows(IllegalArgumentException.class, () -> validator.validate(QuestionType.MCQ_SINGLE, payload));
    }

    @Test
    void validateMcqMulti_Success() {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode options = payload.putArray("options");
        options.addObject().put("id", "1").put("text", "Opt 1");
        options.addObject().put("id", "2").put("text", "Opt 2");
        ArrayNode corrects = payload.putArray("correctOptionIds");
        corrects.add("1").add("2");

        assertDoesNotThrow(() -> validator.validate(QuestionType.MCQ_MULTI, payload));
    }

    @Test
    void validateTrueFalse_Success() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("correctAnswer", true);

        assertDoesNotThrow(() -> validator.validate(QuestionType.TRUE_FALSE, payload));
    }

    @Test
    void validateMatch_Fail_UnequalCounts() {
        ObjectNode payload = mapper.createObjectNode();
        payload.putArray("leftItems").addObject().put("id", "L1");
        payload.putArray("rightItems"); // empty
        payload.putArray("correctPairs");

        assertThrows(IllegalArgumentException.class, () -> validator.validate(QuestionType.MATCH, payload));
    }
}
