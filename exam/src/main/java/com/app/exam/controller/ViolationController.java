package com.app.exam.controller;

import com.app.exam.domain.StudentAttempt;
import com.app.exam.dto.ViolationRequest;
import com.app.exam.dto.ViolationResponse;
import com.app.exam.repository.StudentAttemptRepository;
import com.app.exam.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attempts")
@RequiredArgsConstructor
public class ViolationController {

    private final ViolationService violationService;
    private final StudentAttemptRepository attemptRepository;

    @PostMapping("/{attemptId}/violations")
    public ResponseEntity<ViolationResponse> recordViolation(@PathVariable UUID attemptId, @RequestBody ViolationRequest request) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        
        if (!attempt.getStudentId().equals(studentId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(violationService.recordViolation(attemptId, request));
    }
}
