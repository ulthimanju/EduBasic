package com.app.exam.controller;

import com.app.exam.dto.ResultResponse;
import com.app.exam.service.ResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/{attemptId}")
    public ResponseEntity<ResultResponse> getResult(@PathVariable UUID attemptId) {
        UUID studentId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            return ResponseEntity.ok(resultService.getResultByAttemptId(studentId, attemptId));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Result not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
}
