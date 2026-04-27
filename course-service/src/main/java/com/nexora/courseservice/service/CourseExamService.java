package com.nexora.courseservice.service;

import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.dto.request.LinkExamRequest;
import com.nexora.courseservice.dto.response.CourseExamResponse;
import com.nexora.courseservice.entity.Course;
import com.nexora.courseservice.entity.CourseExam;
import com.nexora.courseservice.entity.EnrollmentStatus;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.CourseEnrollmentRepository;
import com.nexora.courseservice.repository.CourseExamRepository;
import com.nexora.courseservice.repository.CourseRepository;
import com.nexora.courseservice.security.ExamServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseExamService {

    private final CourseExamRepository courseExamRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final ExamServiceClient examServiceClient;

    @Transactional
    public CourseExamResponse linkExam(UUID courseId, LinkExamRequest request, UUID instructorId, String bearerToken) {
        Course course = courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (courseExamRepository.existsByCourseIdAndExamId(courseId, request.getExamId())) {
            throw new CourseServiceException("Exam already linked to course", "ALREADY_LINKED", HttpStatus.CONFLICT);
        }

        if (!examServiceClient.isExamPublished(request.getExamId(), bearerToken)) {
            throw new CourseServiceException(ErrorMessages.EXAM_NOT_PUBLISHED, "EXAM_NOT_PUBLISHED", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        CourseExam courseExam = new CourseExam();
        courseExam.setCourse(course);
        courseExam.setExamId(request.getExamId());
        courseExam.setTitle(request.getTitle());
        courseExam.setOrderIndex(request.getOrderIndex());
        courseExam.setRequiredToComplete(request.isRequiredToComplete());
        courseExam.setMinPassPercent(request.getMinPassPercent());

        return mapToResponse(courseExamRepository.save(courseExam));
    }

    @Transactional
    public void unlinkExam(UUID courseId, UUID examId, UUID instructorId) {
        if (!courseRepository.existsByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId)) {
            throw new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        courseExamRepository.deleteByCourseIdAndExamId(courseId, examId);
    }

    @Transactional(readOnly = true)
    public List<CourseExamResponse> listExams(UUID courseId, UUID requesterId) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        boolean isInstructor = course.getCreatedBy().equals(requesterId);
        boolean isEnrolled = enrollmentRepository.findByCourseIdAndStudentId(courseId, requesterId)
                .map(e -> e.getStatus() == EnrollmentStatus.ACTIVE || e.getStatus() == EnrollmentStatus.COMPLETED)
                .orElse(false);

        if (!isInstructor && !isEnrolled) {
            throw new CourseServiceException("You must be enrolled to view course exams", "NOT_ENROLLED", HttpStatus.FORBIDDEN);
        }

        return courseExamRepository.findByCourseIdOrderByOrderIndex(courseId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CourseExamResponse mapToResponse(CourseExam exam) {
        CourseExamResponse res = new CourseExamResponse();
        res.setId(exam.getId());
        res.setExamId(exam.getExamId());
        res.setTitle(exam.getTitle());
        res.setOrderIndex(exam.getOrderIndex());
        res.setRequiredToComplete(exam.isRequiredToComplete());
        res.setMinPassPercent(exam.getMinPassPercent());
        return res;
    }
}
