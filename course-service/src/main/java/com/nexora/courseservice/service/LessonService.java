package com.nexora.courseservice.service;

import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.constants.ValidationMessages;
import com.nexora.courseservice.dto.request.CreateLessonRequest;
import com.nexora.courseservice.dto.request.ReorderRequest;
import com.nexora.courseservice.dto.request.UpdateLessonRequest;
import com.nexora.courseservice.dto.response.LessonResponse;
import com.nexora.courseservice.entity.ContentType;
import com.nexora.courseservice.entity.CourseModule;
import com.nexora.courseservice.entity.Lesson;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.CourseModuleRepository;
import com.nexora.courseservice.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseModuleRepository moduleRepository;

    @Transactional
    public LessonResponse addLesson(UUID moduleId, CreateLessonRequest request, UUID instructorId) {
        CourseModule module = moduleRepository.findByIdAndIsDeletedFalse(moduleId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.MODULE_NOT_FOUND, "MODULE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!module.getCourse().getCreatedBy().equals(instructorId)) {
            throw new CourseServiceException(ErrorMessages.UNAUTHORIZED_ACCESS, "UNAUTHORIZED_ACCESS", HttpStatus.FORBIDDEN);
        }

        validateLessonContent(request.getContentType(), request.getContentBody(), request.getContentUrl());

        if (lessonRepository.existsByModuleIdAndOrderIndexAndIsDeletedFalse(moduleId, request.getOrderIndex())) {
            throw new CourseServiceException("Order index " + request.getOrderIndex() + " already exists", "VALIDATION_FAILED", HttpStatus.CONFLICT);
        }

        Lesson lesson = new Lesson();
        lesson.setModule(module);
        lesson.setTitle(request.getTitle());
        lesson.setContentType(request.getContentType());
        lesson.setContentBody(request.getContentBody());
        lesson.setContentUrl(request.getContentUrl());
        lesson.setDurationMinutes(request.getDurationMinutes());
        lesson.setOrderIndex(request.getOrderIndex());
        lesson.setPreview(request.isPreview());

        return mapToResponse(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse updateLesson(UUID lessonId, UpdateLessonRequest request, UUID instructorId) {
        Lesson lesson = lessonRepository.findByIdAndIsDeletedFalse(lessonId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.LESSON_NOT_FOUND, "LESSON_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!lesson.getModule().getCourse().getCreatedBy().equals(instructorId)) {
            throw new CourseServiceException(ErrorMessages.UNAUTHORIZED_ACCESS, "UNAUTHORIZED_ACCESS", HttpStatus.FORBIDDEN);
        }

        if (request.getTitle() != null) lesson.setTitle(request.getTitle());
        
        ContentType newType = request.getContentType() != null ? request.getContentType() : lesson.getContentType();
        String newBody = request.getContentBody() != null ? request.getContentBody() : lesson.getContentBody();
        String newUrl = request.getContentUrl() != null ? request.getContentUrl() : lesson.getContentUrl();
        
        if (request.getContentType() != null || request.getContentBody() != null || request.getContentUrl() != null) {
            validateLessonContent(newType, newBody, newUrl);
            lesson.setContentType(newType);
            lesson.setContentBody(newBody);
            lesson.setContentUrl(newUrl);
        }

        if (request.getDurationMinutes() != null) lesson.setDurationMinutes(request.getDurationMinutes());
        if (request.getIsPreview() != null) lesson.setPreview(request.getIsPreview());

        if (request.getOrderIndex() != null && request.getOrderIndex() != lesson.getOrderIndex()) {
            if (lessonRepository.existsByModuleIdAndOrderIndexAndIsDeletedFalse(lesson.getModule().getId(), request.getOrderIndex())) {
                throw new CourseServiceException("Order index " + request.getOrderIndex() + " already exists", "VALIDATION_FAILED", HttpStatus.CONFLICT);
            }
            lesson.setOrderIndex(request.getOrderIndex());
        }

        return mapToResponse(lessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(UUID lessonId, UUID instructorId) {
        Lesson lesson = lessonRepository.findByIdAndIsDeletedFalse(lessonId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.LESSON_NOT_FOUND, "LESSON_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!lesson.getModule().getCourse().getCreatedBy().equals(instructorId)) {
            throw new CourseServiceException(ErrorMessages.UNAUTHORIZED_ACCESS, "UNAUTHORIZED_ACCESS", HttpStatus.FORBIDDEN);
        }

        lesson.setDeleted(true);
        lessonRepository.save(lesson);
    }

    @Transactional
    public void reorderLessons(UUID moduleId, ReorderRequest request, UUID instructorId) {
        CourseModule module = moduleRepository.findByIdAndIsDeletedFalse(moduleId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.MODULE_NOT_FOUND, "MODULE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!module.getCourse().getCreatedBy().equals(instructorId)) {
            throw new CourseServiceException(ErrorMessages.UNAUTHORIZED_ACCESS, "UNAUTHORIZED_ACCESS", HttpStatus.FORBIDDEN);
        }

        List<Lesson> lessons = lessonRepository.findByModuleIdAndIsDeletedFalseOrderByOrderIndex(moduleId);
        Map<UUID, Lesson> lessonMap = lessons.stream()
                .collect(Collectors.toMap(Lesson::getId, l -> l));

        for (ReorderRequest.ReorderItem item : request.getItems()) {
            if (!lessonMap.containsKey(item.id())) {
                throw new CourseServiceException("Lesson " + item.id() + " not found in module", "VALIDATION_FAILED", HttpStatus.BAD_REQUEST);
            }
        }

        request.getItems().forEach(item -> {
            Lesson l = lessonMap.get(item.id());
            l.setOrderIndex(item.orderIndex());
        });

        lessonRepository.saveAll(lessons);
    }

    private void validateLessonContent(ContentType type, String body, String url) {
        if (type == ContentType.TEXT && (body == null || body.isBlank())) {
            throw new CourseServiceException("Content body is required for TEXT type", "VALIDATION_FAILED", HttpStatus.BAD_REQUEST);
        }
        if (type == ContentType.LINK && (url == null || url.isBlank())) {
            throw new CourseServiceException("Content URL is required for LINK type", "VALIDATION_FAILED", HttpStatus.BAD_REQUEST);
        }
        if (type == ContentType.ASSIGNMENT && (url == null || url.isBlank())) {
            throw new CourseServiceException("Content URL (submission link) is required for ASSIGNMENT type", "VALIDATION_FAILED", HttpStatus.BAD_REQUEST);
        }
    }

    private LessonResponse mapToResponse(Lesson lesson) {
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
}
