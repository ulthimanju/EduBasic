package com.app.exam.controller;

import com.app.exam.dto.AttemptResponse;
import com.app.exam.dto.CertificateResponse;
import com.app.exam.dto.StartAttemptRequest;
import com.app.exam.dto.SyncAttemptRequest;
import com.app.exam.service.AttemptService;
import com.app.exam.service.CertificateService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
        return new ResponseEntity<>(attemptService.startAttempt(studentId, request.getExamId()), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/sync")
    @Timed(value = "exam.attempt.sync", description = "Time taken to sync attempt answers")
    public ResponseEntity<AttemptResponse> syncAttempt(@PathVariable UUID id, @Valid @RequestBody SyncAttemptRequest request) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(attemptService.syncAttempt(studentId, id, request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submitAttempt(@PathVariable UUID id) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        attemptService.submitAttempt(studentId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttemptResponse> getAttempt(@PathVariable UUID id) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(attemptService.getAttempt(studentId, id));
    }

    @GetMapping("/{id}/certificate")
    public ResponseEntity<CertificateResponse> getCertificate(@PathVariable UUID id) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(certificateService.getCertificate(studentId, id));
    }
}
