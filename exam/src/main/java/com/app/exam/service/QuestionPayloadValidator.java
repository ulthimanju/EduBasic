package com.app.exam.service;

import com.app.exam.domain.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class QuestionPayloadValidator {

    public void validate(QuestionType type, JsonNode payload) {
        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("Payload cannot be null");
        }

        switch (type) {
            case MCQ_SINGLE -> validateMcqSingle(payload);
            case MCQ_MULTI -> validateMcqMulti(payload);
            case TRUE_FALSE -> validateTrueFalse(payload);
            case FILL_BLANK -> validateFillBlank(payload);
            case MATCH -> validateMatch(payload);
            case SEQUENCE -> validateSequence(payload);
            case CODING -> validateCoding(payload);
            case SUBJECTIVE -> validateSubjective(payload);
            default -> throw new IllegalArgumentException("Unsupported question type: " + type);
        }
    }

    private void validateMcqSingle(JsonNode payload) {
        validateOptions(payload);
        if (!payload.has("correctOptionId") || payload.get("correctOptionId").asText().isEmpty()) {
            throw new IllegalArgumentException("MCQ_SINGLE must have exactly one correctOptionId");
        }
        String correctId = payload.get("correctOptionId").asText();
        boolean found = false;
        for (JsonNode option : payload.get("options")) {
            if (option.get("id").asText().equals(correctId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("correctOptionId must match one of the options");
        }
    }

    private void validateMcqMulti(JsonNode payload) {
        validateOptions(payload);
        if (!payload.has("correctOptionIds") || !payload.get("correctOptionIds").isArray() || payload.get("correctOptionIds").size() < 2) {
            throw new IllegalArgumentException("MCQ_MULTI must have at least 2 correctOptionIds");
        }
        Set<String> optionIds = new HashSet<>();
        for (JsonNode option : payload.get("options")) {
            optionIds.add(option.get("id").asText());
        }
        for (JsonNode idNode : payload.get("correctOptionIds")) {
            if (!optionIds.contains(idNode.asText())) {
                throw new IllegalArgumentException("All correctOptionIds must match one of the options");
            }
        }
    }

    private void validateTrueFalse(JsonNode payload) {
        if (!payload.has("correctAnswer") || !payload.get("correctAnswer").isBoolean()) {
            throw new IllegalArgumentException("TRUE_FALSE must have a boolean correctAnswer");
        }
    }

    private void validateFillBlank(JsonNode payload) {
        if (!payload.has("blanks") || !payload.get("blanks").isArray() || payload.get("blanks").isEmpty()) {
            throw new IllegalArgumentException("FILL_BLANK must have at least one blank");
        }
        for (JsonNode blank : payload.get("blanks")) {
            if (!blank.has("id") || !blank.has("acceptedAnswers") || !blank.get("acceptedAnswers").isArray() || blank.get("acceptedAnswers").isEmpty()) {
                throw new IllegalArgumentException("Each blank must have an id and at least one acceptedAnswer");
            }
        }
    }

    private void validateMatch(JsonNode payload) {
        if (!payload.has("leftItems") || !payload.get("leftItems").isArray() ||
            !payload.has("rightItems") || !payload.get("rightItems").isArray() ||
            !payload.has("correctPairs") || !payload.get("correctPairs").isArray()) {
            throw new IllegalArgumentException("MATCH must have leftItems, rightItems, and correctPairs");
        }
        if (payload.get("leftItems").size() != payload.get("rightItems").size()) {
            throw new IllegalArgumentException("MATCH left and right item counts must be equal");
        }
    }

    private void validateSequence(JsonNode payload) {
        if (!payload.has("items") || !payload.get("items").isArray() ||
            !payload.has("correctOrder") || !payload.get("correctOrder").isArray()) {
            throw new IllegalArgumentException("SEQUENCE must have items and correctOrder");
        }
        if (payload.get("items").size() != payload.get("correctOrder").size()) {
            throw new IllegalArgumentException("SEQUENCE correctOrder length must match items length");
        }
    }

    private void validateCoding(JsonNode payload) {
        if (!payload.has("languagesAllowed") || !payload.get("languagesAllowed").isArray() ||
            !payload.has("testCases") || !payload.get("testCases").isArray() || payload.get("testCases").isEmpty()) {
            throw new IllegalArgumentException("CODING must have languagesAllowed and at least one testCase");
        }
        boolean hasNonHidden = false;
        for (JsonNode tc : payload.get("testCases")) {
            if (tc.has("isHidden") && !tc.get("isHidden").asBoolean()) {
                hasNonHidden = true;
                break;
            }
        }
        if (!hasNonHidden) {
            throw new IllegalArgumentException("CODING must have at least one non-hidden test case");
        }
    }

    private void validateSubjective(JsonNode payload) {
        if (!payload.has("maxWords") || !payload.get("maxWords").isInt()) {
            throw new IllegalArgumentException("SUBJECTIVE must have maxWords");
        }
    }

    private void validateOptions(JsonNode payload) {
        if (!payload.has("options") || !payload.get("options").isArray() || payload.get("options").size() < 2) {
            throw new IllegalArgumentException("MCQ must have at least 2 options");
        }
        for (JsonNode option : payload.get("options")) {
            if (!option.has("id") || !option.has("text")) {
                throw new IllegalArgumentException("Each option must have an id and text");
            }
        }
    }
}
