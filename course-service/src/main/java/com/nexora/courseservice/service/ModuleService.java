package com.nexora.courseservice.service;

import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.dto.request.CreateModuleRequest;
import com.nexora.courseservice.dto.request.ReorderRequest;
import com.nexora.courseservice.dto.request.UpdateModuleRequest;
import com.nexora.courseservice.dto.response.LessonResponse;
import com.nexora.courseservice.dto.response.ModuleResponse;
import com.nexora.courseservice.entity.Course;
import com.nexora.courseservice.entity.CourseModule;
import com.nexora.courseservice.entity.CourseStatus;
import com.nexora.courseservice.entity.Lesson;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.CourseModuleRepository;
import com.nexora.courseservice.repository.CourseRepository;
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
public class ModuleService {

    private final CourseModuleRepository moduleRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public ModuleResponse addModule(UUID courseId, CreateModuleRequest request, UUID instructorId) {
        Course course = courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new CourseServiceException(ErrorMessages.INVALID_STATUS_TRANSITION, "INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        if (moduleRepository.existsByCourseIdAndOrderIndexAndIsDeletedFalse(courseId, request.getOrderIndex())) {
            throw new CourseServiceException("Order index " + request.getOrderIndex() + " already exists", "VALIDATION_FAILED", HttpStatus.CONFLICT);
        }

        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());
        module.setOrderIndex(request.getOrderIndex());

        return mapToResponse(moduleRepository.save(module));
    }

    @Transactional
    public ModuleResponse updateModule(UUID moduleId, UpdateModuleRequest request, UUID instructorId) {
        CourseModule module = moduleRepository.findByIdAndIsDeletedFalse(moduleId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.MODULE_NOT_FOUND, "MODULE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!module.getCourse().getCreatedBy().equals(instructorId)) {
            throw new CourseServiceException(ErrorMessages.UNAUTHORIZED_ACCESS, "UNAUTHORIZED_ACCESS", HttpStatus.FORBIDDEN);
        }

        if (request.getTitle() != null) module.setTitle(request.getTitle());
        if (request.getDescription() != null) module.setDescription(request.getDescription());
        
        if (request.getOrderIndex() != null && request.getOrderIndex() != module.getOrderIndex()) {
            if (moduleRepository.existsByCourseIdAndOrderIndexAndIsDeletedFalse(module.getCourse().getId(), request.getOrderIndex())) {
                throw new CourseServiceException("Order index " + request.getOrderIndex() + " already exists", "VALIDATION_FAILED", HttpStatus.CONFLICT);
            }
            module.setOrderIndex(request.getOrderIndex());
        }

        return mapToResponse(moduleRepository.save(module));
    }

    @Transactional
    public void deleteModule(UUID moduleId, UUID instructorId) {
        CourseModule module = moduleRepository.findByIdAndIsDeletedFalse(moduleId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.MODULE_NOT_FOUND, "MODULE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!module.getCourse().getCreatedBy().equals(instructorId)) {
            throw new CourseServiceException(ErrorMessages.UNAUTHORIZED_ACCESS, "UNAUTHORIZED_ACCESS", HttpStatus.FORBIDDEN);
        }

        module.setDeleted(true);
        module.getLessons().forEach(l -> l.setDeleted(true));
        moduleRepository.save(module);
    }

    @Transactional
    public void reorderModules(UUID courseId, ReorderRequest request, UUID instructorId) {
        Course course = courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<CourseModule> modules = moduleRepository.findByCourseIdAndIsDeletedFalseOrderByOrderIndex(courseId);
        Map<UUID, CourseModule> moduleMap = modules.stream()
                .collect(Collectors.toMap(CourseModule::getId, m -> m));

        // Validate all IDs in request belong to course
        for (ReorderRequest.ReorderItem item : request.getItems()) {
            if (!moduleMap.containsKey(item.id())) {
                throw new CourseServiceException("Module " + item.id() + " not found in course", "VALIDATION_FAILED", HttpStatus.BAD_REQUEST);
            }
        }

        // Apply new indices
        request.getItems().forEach(item -> {
            CourseModule m = moduleMap.get(item.id());
            m.setOrderIndex(item.orderIndex());
        });

        moduleRepository.saveAll(modules);
    }

    private ModuleResponse mapToResponse(CourseModule module) {
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
}
