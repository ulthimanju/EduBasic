package com.edubas.backend.config;

import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {

    public JacksonConfig(ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());
    }
}
