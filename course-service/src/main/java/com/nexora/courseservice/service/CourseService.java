package com.nexora.courseservice.service;

import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.constants.LogMessages;
import com.nexora.courseservice.dto.request.CreateCourseRequest;
import com.nexora.courseservice.dto.request.UpdateCourseRequest;
import com.nexora.courseservice.dto.response.*;
import com.nexora.courseservice.entity.*;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.CourseRepository;
import com.nexora.courseservice.security.ExamServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final ExamServiceClient examServiceClient;

    @Transactional
    public CourseSummaryResponse createCourse(CreateCourseRequest request, UUID instructorId) {
        Course course = new Course();
        course.setCreatedBy(instructorId);
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(request.getThumbnailUrl());
        
        if (request.getCompletionRules() != null) {
            course.setCompletionRules(request.getCompletionRules());
        }

        Course saved = courseRepository.save(course);
        log.info(LogMessages.COURSE_CREATED, saved.getId());
        return mapToSummary(saved);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(UUID courseId, UUID requesterId) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));
        
        return mapToResponse(course);
    }

    @Transactional(readOnly = true)
    public Page<CourseSummaryResponse> listMyCourses(UUID instructorId, Pageable pageable) {
        return courseRepository.findByCreatedByAndIsDeletedFalse(instructorId, pageable)
                .map(this::mapToSummary);
    }

    @Transactional
    public CourseResponse updateCourse(UUID courseId, UpdateCourseRequest request, UUID instructorId) {
        Course course = courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new CourseServiceException(ErrorMessages.INVALID_STATUS_TRANSITION, "INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        if (request.getTitle() != null) course.setTitle(request.getTitle());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getThumbnailUrl() != null) course.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getCompletionRules() != null) course.setCompletionRules(request.getCompletionRules());

        return mapToResponse(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(UUID courseId, UUID instructorId) {
        Course course = courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new CourseServiceException(ErrorMessages.INVALID_STATUS_TRANSITION, "INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        course.setDeleted(true);
        courseRepository.save(course);
    }

    @Transactional
    public CourseResponse publishCourse(UUID courseId, UUID instructorId, String bearerToken) {
        Course course = courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new CourseServiceException(ErrorMessages.INVALID_STATUS_TRANSITION, "INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        // 1. Validate Modules
        long moduleCount = course.getModules().stream().filter(m -> !m.isDeleted()).count();
        if (moduleCount == 0) {
            throw new CourseServiceException("Course must have at least one module", "VALIDATION_FAILED", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // 2. Validate Lessons
        for (CourseModule module : course.getModules()) {
            if (!module.isDeleted()) {
                long lessonCount = module.getLessons().stream().filter(l -> !l.isDeleted()).count();
                if (lessonCount == 0) {
                    throw new CourseServiceException("Module '" + module.getTitle() + "' has no lessons", "VALIDATION_FAILED", HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
        }

        // 3. Validate Exams
        for (CourseExam exam : course.getCourseExams()) {
            if (exam.isRequiredToComplete()) {
                if (!examServiceClient.isExamPublished(exam.getExamId(), bearerToken)) {
                    throw new CourseServiceException(ErrorMessages.EXAM_NOT_PUBLISHED, "EXAM_NOT_PUBLISHED", HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
        }

        course.setStatus(CourseStatus.PUBLISHED);
        Course saved = courseRepository.save(course);
        log.info(LogMessages.COURSE_PUBLISHED, saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public CourseResponse archiveCourse(UUID courseId, UUID instructorId) {
        Course course = courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new CourseServiceException(ErrorMessages.INVALID_STATUS_TRANSITION, "INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        course.setStatus(CourseStatus.ARCHIVED);
        return mapToResponse(courseRepository.save(course));
    }

    private CourseSummaryResponse mapToSummary(Course course) {
        CourseSummaryResponse res = new CourseSummaryResponse();
        res.setId(course.getId());
        res.setTitle(course.getTitle());
        res.setDescription(course.getDescription());
        res.setThumbnailUrl(course.getThumbnailUrl());
        res.setStatus(course.getStatus());
        res.setCreatedAt(course.getCreatedAt());
        
        long moduleCount = course.getModules().stream().filter(m -> !m.isDeleted()).count();
        res.setTotalModules((int) moduleCount);
        
        long lessonCount = course.getModules().stream()
                .filter(m -> !m.isDeleted())
                .flatMap(m -> m.getLessons().stream())
                .filter(l -> !l.isDeleted())
                .count();
        res.setTotalLessons((int) lessonCount);
        
        return res;
    }

    private CourseResponse mapToResponse(Course course) {
        CourseResponse res = new CourseResponse();
        res.setId(course.getId());
        res.setTitle(course.getTitle());
        res.setDescription(course.getDescription());
        res.setThumbnailUrl(course.getThumbnailUrl());
        res.setStatus(course.getStatus());
        res.setCompletionRules(course.getCompletionRules());
        res.setCreatedBy(course.getCreatedBy());
        res.setCreatedAt(course.getCreatedAt());
        res.setUpdatedAt(course.getUpdatedAt());
        
        res.setModules(course.getModules().stream()
                .filter(m -> !m.isDeleted())
                .map(this::mapToModuleResponse)
                .collect(Collectors.toList()));
        
        res.setExams(course.getCourseExams().stream()
                .map(this::mapToExamResponse)
                .collect(Collectors.toList()));
        
        return res;
    }

    private ModuleResponse mapToModuleResponse(CourseModule module) {
        ModuleResponse res = new ModuleResponse();
        res.setId(module.getId());
        res.setTitle(module.getTitle());
        res.setDescription(module.getDescription());
        res.setOrderIndex(module.getOrderIndex());
        res.setLessons(module.getLessons().stream()
                .filter(l -> !l.isDeleted())
                .map(this::mapToLessonResponse)
                .collect(Collectors.toList()));
        return res;
    }

    private LessonResponse mapToLessonResponse(Lesson lesson) {
        LessonResponse res = new LessonResponse();
        res.setId(lesson.getId());
        res.setTitle(lesson.getTitle());
        res.setContentType(lesson.getContentType());
        res.setContentBody(lesson.getContentBody());
        res.setContentUrl(lesson.getContentUrl());
        res.setDurationMinutes(lesson.getDurationMinutes());
        res.setOrderIndex(lesson.getOrderIndex());
        res.setPreview(lesson.isPreview());
        return res;
    }

    private CourseExamResponse mapToExamResponse(CourseExam exam) {
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
