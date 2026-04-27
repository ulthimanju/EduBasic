package com.app.exam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProctoringConsumer {

    private final ProctoringService proctoringService;

    @KafkaListener(topics = "proctoring-events", groupId = "proctoring-group")
    public void consumeProctoringEvent(Map<String, Object> event) {
        UUID attemptId = UUID.fromString((String) event.get("attemptId"));
        String eventType = (String) event.get("eventType");
        
        Map<String, Object> eventData = event.get("eventData") instanceof Map<?, ?> rawMap
            ? rawMap.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue))
            : Map.of();

        log.info("Received proctoring event {} for attempt: {}", eventType, attemptId);
        
        try {
            proctoringService.logEvent(attemptId, eventType, eventData);
        } catch (Exception e) {
            log.error("Failed to process proctoring event for attempt {}", attemptId, e);
            throw e; // Rethrow to trigger Kafka retry
        }
    }
}
