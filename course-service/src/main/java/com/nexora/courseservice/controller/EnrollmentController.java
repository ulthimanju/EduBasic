package com.nexora.courseservice.controller;

import com.nexora.courseservice.dto.response.CompletionStatusResponse;
import com.nexora.courseservice.dto.response.CourseOutlineResponse;
import com.nexora.courseservice.dto.response.EnrollmentResponse;
import com.nexora.courseservice.dto.response.MyCourseSummaryResponse;
import com.nexora.courseservice.service.CompletionCheckService;
import com.nexora.courseservice.service.EnrollmentService;
import com.nexora.courseservice.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final ProgressService progressService;
    private final CompletionCheckService completionCheckService;

    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UUID studentId) {
        return new ResponseEntity<>(enrollmentService.enroll(courseId, studentId), HttpStatus.CREATED);
    }

    @DeleteMapping("/courses/{courseId}/enroll")
    public ResponseEntity<Void> dropEnrollment(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UUID studentId) {
        enrollmentService.dropEnrollment(courseId, studentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/courses")
    public ResponseEntity<Page<MyCourseSummaryResponse>> getMyEnrolledCourses(
            @AuthenticationPrincipal UUID studentId,
            Pageable pageable) {
        return ResponseEntity.ok(enrollmentService.getMyEnrolledCourses(studentId, pageable));
    }

    @GetMapping("/me/courses/{courseId}")
    public ResponseEntity<CourseOutlineResponse> getCourseOutlineWithProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UUID studentId) {
        return ResponseEntity.ok(progressService.getCourseOutlineWithProgress(courseId, studentId));
    }

    @GetMapping("/me/courses/{courseId}/completion")
    public ResponseEntity<CompletionStatusResponse> getCompletionStatus(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UUID studentId) {
        return ResponseEntity.ok(completionCheckService.getCompletionStatus(courseId, studentId));
    }
}
