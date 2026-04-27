package com.app.exam.controller;

import com.app.exam.domain.ExamStatus;
import com.app.exam.dto.*;
import com.app.exam.service.ExamService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody CreateExamRequest request) {
        return new ResponseEntity<>(examService.createExam(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Timed(value = "exam.list", description = "Time taken to list exams")
    public ResponseEntity<Page<ExamSummaryResponse>> listExams(
            @RequestParam(required = false) ExamStatus status,
            Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = (UUID) auth.getPrincipal();
        boolean isStudent = auth.getAuthorities().contains(new SimpleGrantedAuthority("STUDENT"));

        if (isStudent) {
            // Students can only see PUBLISHED exams
            return ResponseEntity.ok(examService.listExamsByStatus(ExamStatus.PUBLISHED, pageable));
        }

        return ResponseEntity.ok(examService.listExams(userId, status, pageable));
    }

    @GetMapping("/{id}")
    @Timed(value = "exam.get", description = "Time taken to fetch full exam details")
    public ResponseEntity<ExamResponse> getExam(@PathVariable UUID id) {
        return ResponseEntity.ok(examService.getExam(id));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishExam(@PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID instructorId = (UUID) auth.getPrincipal();
        examService.publishExam(id, instructorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sections")
    public ResponseEntity<Void> addSection(@PathVariable UUID id, @Valid @RequestBody ExamSectionRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID instructorId = (UUID) auth.getPrincipal();
        examService.addSection(id, instructorId, request.getTitle(), request.getDescription(), request.getOrderIndex());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<Void> addQuestion(@PathVariable UUID id, @Valid @RequestBody AddQuestionToExamRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID instructorId = (UUID) auth.getPrincipal();
        examService.addQuestion(id, instructorId, request);
        return ResponseEntity.noContent().build();
    }
}
