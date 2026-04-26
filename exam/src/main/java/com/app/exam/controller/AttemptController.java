package com.app.exam.controller;

import com.app.exam.domain.Certificate;
import com.app.exam.dto.AttemptResponse;
import com.app.exam.dto.StartAttemptRequest;
import com.app.exam.dto.SyncAttemptRequest;
import com.app.exam.service.AttemptService;
import com.app.exam.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;
    private final CertificateService certificateService;

    @PostMapping
    public ResponseEntity<AttemptResponse> startAttempt(@Valid @RequestBody StartAttemptRequest request) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(attemptService.startAttempt(studentId, request.getExamId()));
    }

    @PutMapping("/{id}/sync")
    public ResponseEntity<AttemptResponse> syncAttempt(@PathVariable UUID id, @Valid @RequestBody SyncAttemptRequest request) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(attemptService.syncAttempt(studentId, id, request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submitAttempt(@PathVariable UUID id) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        attemptService.submitAttempt(studentId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/certificate")
    public ResponseEntity<Certificate> getCertificate(@PathVariable UUID id) {
        return ResponseEntity.ok(certificateService.getCertificate(id));
    }
}
