package com.nexora.courseservice.controller;

import com.nexora.courseservice.entity.EnrollmentStatus;
import com.nexora.courseservice.repository.CourseEnrollmentRepository;
import com.nexora.courseservice.repository.CourseExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/courses")
@RequiredArgsConstructor
public class InternalCourseController {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseExamRepository courseExamRepository;

    @GetMapping("/validate-access")
    public ResponseEntity<Boolean> validateStudentAccess(
            @RequestParam UUID studentId,
            @RequestParam UUID examId) {
        
        // Find any course linked to this exam where the student is actively enrolled
        boolean hasAccess = courseExamRepository.findByExamId(examId).stream()
                .anyMatch(ce -> enrollmentRepository.findByCourseIdAndStudentId(ce.getCourse().getId(), studentId)
                        .map(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                        .orElse(false));

        return ResponseEntity.ok(hasAccess);
    }
}
