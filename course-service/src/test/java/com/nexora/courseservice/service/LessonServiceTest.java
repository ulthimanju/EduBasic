package com.nexora.courseservice.service;

import com.nexora.courseservice.dto.request.CreateLessonRequest;
import com.nexora.courseservice.entity.*;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.CourseModuleRepository;
import com.nexora.courseservice.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseModuleRepository moduleRepository;

    @InjectMocks
    private LessonService lessonService;

    private UUID instructorId;
    private UUID moduleId;
    private CourseModule module;

    @BeforeEach
    void setUp() {
        instructorId = UUID.randomUUID();
        moduleId = UUID.randomUUID();
        
        Course course = new Course();
        course.setCreatedBy(instructorId);
        
        module = new CourseModule();
        module.setId(moduleId);
        module.setCourse(course);
    }

    @Test
    void addLesson_ShouldFailIfTextTypeHasNoBody() {
        // Arrange
        CreateLessonRequest request = new CreateLessonRequest();
        request.setContentType(ContentType.TEXT);
        request.setContentBody(""); // Empty body
        request.setTitle("Lesson 1");

        when(moduleRepository.findByIdAndIsDeletedFalse(moduleId)).thenReturn(Optional.of(module));

        // Act & Assert
        CourseServiceException ex = assertThrows(CourseServiceException.class, 
                () -> lessonService.addLesson(moduleId, request, instructorId));
        assertTrue(ex.getMessage().contains("Content body is required"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void addLesson_ShouldFailIfLinkTypeHasNoUrl() {
        // Arrange
        CreateLessonRequest request = new CreateLessonRequest();
        request.setContentType(ContentType.LINK);
        request.setContentUrl(null);
        request.setTitle("Lesson 1");

        when(moduleRepository.findByIdAndIsDeletedFalse(moduleId)).thenReturn(Optional.of(module));

        // Act & Assert
        CourseServiceException ex = assertThrows(CourseServiceException.class, 
                () -> lessonService.addLesson(moduleId, request, instructorId));
        assertTrue(ex.getMessage().contains("Content URL is required"));
    }
}
