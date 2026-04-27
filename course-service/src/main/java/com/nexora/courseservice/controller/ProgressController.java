package com.nexora.courseservice.controller;

import com.nexora.courseservice.dto.request.UpdateProgressRequest;
import com.nexora.courseservice.dto.response.LessonProgressResponse;
import com.nexora.courseservice.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PutMapping("/{lessonId}/progress")
    public ResponseEntity<LessonProgressResponse> updateLessonProgress(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal UUID studentId,
            @Valid @RequestBody UpdateProgressRequest request) {
        return ResponseEntity.ok(progressService.updateLessonProgress(lessonId, studentId, request));
    }

    @GetMapping("/{lessonId}/progress")
    public ResponseEntity<LessonProgressResponse> getLessonProgress(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal UUID studentId) {
        return ResponseEntity.ok(progressService.getLessonProgress(lessonId, studentId));
    }
}
