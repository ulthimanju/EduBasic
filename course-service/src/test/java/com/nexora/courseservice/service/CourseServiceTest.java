package com.nexora.courseservice.service;

import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.dto.request.UpdateCourseRequest;
import com.nexora.courseservice.entity.*;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.CourseRepository;
import com.nexora.courseservice.security.ExamServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ExamServiceClient examServiceClient;

    @InjectMocks
    private CourseService courseService;

    private UUID instructorId;
    private UUID courseId;
    private Course course;

    @BeforeEach
    void setUp() {
        instructorId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        course = new Course();
        course.setId(courseId);
        course.setCreatedBy(instructorId);
        course.setStatus(CourseStatus.DRAFT);
        course.setTitle("Original Title");
    }

    @Test
    void publishCourse_ShouldSucceedWhenValid() {
        // Arrange
        CourseModule module = new CourseModule();
        module.setTitle("Module 1");
        Lesson lesson = new Lesson();
        lesson.setTitle("Lesson 1");
        module.setLessons(List.of(lesson));
        course.setModules(List.of(module));
        
        when(courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId))
                .thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        // Act
        courseService.publishCourse(courseId, instructorId, "token");

        // Assert
        assertEquals(CourseStatus.PUBLISHED, course.getStatus());
        verify(courseRepository).save(course);
    }

    @Test
    void publishCourse_ShouldThrowExceptionWhenNoModules() {
        // Arrange
        course.setModules(List.of());
        when(courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId))
                .thenReturn(Optional.of(course));

        // Act & Assert
        CourseServiceException ex = assertThrows(CourseServiceException.class, 
                () -> courseService.publishCourse(courseId, instructorId, "token"));
        assertEquals("Course must have at least one module", ex.getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }

    @Test
    void updateCourse_ShouldOnlyUpdateNonNullFields() {
        // Arrange
        UpdateCourseRequest request = new UpdateCourseRequest();
        request.setTitle("New Title");
        // description is null in request
        
        when(courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId))
                .thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        courseService.updateCourse(courseId, request, instructorId);

        // Assert
        assertEquals("New Title", course.getTitle());
        assertNull(course.getDescription()); // Should remain null/unchanged
    }

    @Test
    void deleteCourse_ShouldFailIfStatusNotDraft() {
        // Arrange
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findByIdAndCreatedByAndIsDeletedFalse(courseId, instructorId))
                .thenReturn(Optional.of(course));

        // Act & Assert
        CourseServiceException ex = assertThrows(CourseServiceException.class, 
                () -> courseService.deleteCourse(courseId, instructorId));
        assertEquals(ErrorMessages.INVALID_STATUS_TRANSITION, ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
