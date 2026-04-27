package com.nexora.courseservice.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Converter(autoApply = true)
@Slf4j
public class CompletionRulesConverter implements AttributeConverter<CompletionRules, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(CompletionRules attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting CompletionRules to JSON", e);
            throw new RuntimeException("Error converting CompletionRules to JSON", e);
        }
    }

    @Override
    public CompletionRules convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, CompletionRules.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to CompletionRules", e);
            throw new RuntimeException("Error converting JSON to CompletionRules", e);
        }
    }
}
