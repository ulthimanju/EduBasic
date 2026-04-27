package com.nexora.courseservice.controller;

import com.nexora.courseservice.dto.request.CreateCourseRequest;
import com.nexora.courseservice.dto.request.LinkExamRequest;
import com.nexora.courseservice.dto.request.UpdateCourseRequest;
import com.nexora.courseservice.dto.response.CourseExamResponse;
import com.nexora.courseservice.dto.response.CourseResponse;
import com.nexora.courseservice.dto.response.CourseSummaryResponse;
import com.nexora.courseservice.service.CourseExamService;
import com.nexora.courseservice.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseExamService courseExamService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseSummaryResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal UUID instructorId) {
        return new ResponseEntity<>(courseService.createCourse(request, instructorId), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Page<CourseSummaryResponse>> listMyCourses(
            @AuthenticationPrincipal UUID instructorId,
            Pageable pageable) {
        return ResponseEntity.ok(courseService.listMyCourses(instructorId, pageable));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UUID requesterId) {
        return ResponseEntity.ok(courseService.getCourse(courseId, requesterId));
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request,
            @AuthenticationPrincipal UUID instructorId) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, request, instructorId));
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UUID instructorId) {
        courseService.deleteCourse(courseId, instructorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/publish")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponse> publishCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UUID instructorId,
            @RequestHeader("Authorization") String bearerToken) {
        return ResponseEntity.ok(courseService.publishCourse(courseId, instructorId, bearerToken));
    }

    @PostMapping("/{courseId}/archive")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponse> archiveCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UUID instructorId) {
        return ResponseEntity.ok(courseService.archiveCourse(courseId, instructorId));
    }

    @PostMapping("/{courseId}/exams")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseExamResponse> linkExam(
            @PathVariable UUID courseId,
            @Valid @RequestBody LinkExamRequest request,
            @AuthenticationPrincipal UUID instructorId,
            @RequestHeader("Authorization") String bearerToken) {
        return new ResponseEntity<>(courseExamService.linkExam(courseId, request, instructorId, bearerToken), HttpStatus.CREATED);
    }

    @DeleteMapping("/{courseId}/exams/{examId}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> unlinkExam(
            @PathVariable UUID courseId,
            @PathVariable UUID examId,
            @AuthenticationPrincipal UUID instructorId) {
        courseExamService.unlinkExam(courseId, examId, instructorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courseId}/exams")
    public ResponseEntity<List<CourseExamResponse>> listExams(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseExamService.listExams(courseId));
    }
}
