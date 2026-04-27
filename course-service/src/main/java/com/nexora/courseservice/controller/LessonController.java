package com.nexora.courseservice.controller;

import com.nexora.courseservice.dto.request.CreateLessonRequest;
import com.nexora.courseservice.dto.request.ReorderRequest;
import com.nexora.courseservice.dto.request.UpdateLessonRequest;
import com.nexora.courseservice.dto.response.LessonResponse;
import com.nexora.courseservice.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping("/modules/{moduleId}/lessons")
    public ResponseEntity<LessonResponse> addLesson(
            @PathVariable UUID moduleId,
            @Valid @RequestBody CreateLessonRequest request,
            @AuthenticationPrincipal UUID instructorId) {
        return new ResponseEntity<>(lessonService.addLesson(moduleId, request, instructorId), HttpStatus.CREATED);
    }

    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateLessonRequest request,
            @AuthenticationPrincipal UUID instructorId) {
        return ResponseEntity.ok(lessonService.updateLesson(lessonId, request, instructorId));
    }

    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal UUID instructorId) {
        lessonService.deleteLesson(lessonId, instructorId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/modules/{moduleId}/lessons/reorder")
    public ResponseEntity<Void> reorderLessons(
            @PathVariable UUID moduleId,
            @Valid @RequestBody ReorderRequest request,
            @AuthenticationPrincipal UUID instructorId) {
        lessonService.reorderLessons(moduleId, request, instructorId);
        return ResponseEntity.ok().build();
    }
}
