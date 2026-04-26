package com.app.exam.controller;

import com.app.exam.domain.ProctoringLog;
import com.app.exam.domain.StudentAttempt;
import com.app.exam.dto.ProctoringEventRequest;
import com.app.exam.repository.StudentAttemptRepository;
import com.app.exam.service.ProctoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/proctoring")
@RequiredArgsConstructor
public class ProctoringController {

    private final ProctoringService proctoringService;
    private final StudentAttemptRepository attemptRepository;

    @PostMapping("/attempts/{attemptId}/log")
    public ResponseEntity<Void> logEvent(@PathVariable UUID attemptId, @RequestBody ProctoringEventRequest request) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        
        if (!attempt.getStudentId().equals(studentId)) {
            return ResponseEntity.status(403).build();
        }

        proctoringService.logEvent(attemptId, request.getEventType(), request.getEventData());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/attempts/{attemptId}/logs")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<ProctoringLog>> getLogs(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(proctoringService.getLogs(attemptId));
    }
}
