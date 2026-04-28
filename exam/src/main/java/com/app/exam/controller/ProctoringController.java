package com.app.exam.controller;

import com.app.exam.domain.ProctoringLog;
import com.app.exam.domain.StudentAttempt;
import com.app.exam.dto.ProctoringEventRequest;
import com.app.exam.repository.StudentAttemptRepository;
import com.app.exam.service.ProctoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/proctoring")
@RequiredArgsConstructor
@Slf4j
public class ProctoringController {

    private final ProctoringService proctoringService;
    private final StudentAttemptRepository attemptRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping("/attempts/{attemptId}/log")
    public ResponseEntity<Void> logEvent(@PathVariable UUID attemptId, @Valid @RequestBody ProctoringEventRequest request) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // We still do a quick check if the attempt exists and belongs to the student
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        
        if (!attempt.getStudentId().equals(studentId)) {
            return ResponseEntity.status(403).build();
        }

        // Publish to Kafka for async processing
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put("attemptId", attemptId.toString());
        event.put("eventType", request.getEventType());
        event.put("eventData", request.getEventData());
        event.put("timestamp", java.time.OffsetDateTime.now().toString());

        kafkaTemplate.send("proctoring-events", attemptId.toString(), event)
            .whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish proctoring event for attempt: {}", attemptId, ex);
                }
            });
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/attempts/{attemptId}/logs")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Page<ProctoringLog>> getLogs(
            @PathVariable UUID attemptId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(proctoringService.getLogs(attemptId, pageable));
    }
}
