package com.nexora.courseservice.service;

import com.nexora.courseservice.dto.request.UpdateProgressRequest;
import com.nexora.courseservice.dto.response.LessonProgressResponse;
import com.nexora.courseservice.entity.*;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private LessonProgressRepository progressRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private CompletionCheckService completionCheckService;

    @InjectMocks
    private ProgressService progressService;

    private UUID studentId;
    private UUID lessonId;
    private Lesson lesson;
    private CourseEnrollment enrollment;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        lessonId = UUID.randomUUID();
        
        Course course = new Course();
        course.setId(UUID.randomUUID());
        
        CourseModule module = new CourseModule();
        module.setCourse(course);
        
        lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setModule(module);
        
        enrollment = new CourseEnrollment();
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
    }

    @Test
    void updateLessonProgress_ShouldSetCompleted_WhenPercentIs100() {
        // Arrange
        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setProgressPercent(100);

        when(lessonRepository.findByIdAndIsDeletedFalse(lessonId)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findByCourseIdAndStudentId(any(), eq(studentId))).thenReturn(Optional.of(enrollment));
        when(progressRepository.findByLessonIdAndStudentId(lessonId, studentId)).thenReturn(Optional.empty());
        when(progressRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Act
        LessonProgressResponse response = progressService.updateLessonProgress(lessonId, studentId, request);

        // Assert
        assertEquals(ProgressStatus.COMPLETED, response.getStatus());
        assertEquals(100, response.getProgressPercent());
        assertNotNull(response.getCompletedAt());
        verify(completionCheckService).checkAndComplete(any(), eq(studentId));
    }

    @Test
    void updateLessonProgress_ShouldNotRegress() {
        // Arrange
        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setProgressPercent(50);

        LessonProgress existing = new LessonProgress();
        existing.setProgressPercent(80);
        existing.setStatus(ProgressStatus.IN_PROGRESS);

        when(lessonRepository.findByIdAndIsDeletedFalse(lessonId)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findByCourseIdAndStudentId(any(), eq(studentId))).thenReturn(Optional.of(enrollment));
        when(progressRepository.findByLessonIdAndStudentId(lessonId, studentId)).thenReturn(Optional.of(existing));

        // Act
        LessonProgressResponse response = progressService.updateLessonProgress(lessonId, studentId, request);

        // Assert
        assertEquals(80, response.getProgressPercent()); // Should remain 80
        verify(progressRepository, never()).save(any());
    }

    @Test
    void updateLessonProgress_ShouldThrowException_WhenNotEnrolled() {
        // Arrange
        UpdateProgressRequest request = new UpdateProgressRequest();
        request.setProgressPercent(50);

        when(lessonRepository.findByIdAndIsDeletedFalse(lessonId)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findByCourseIdAndStudentId(any(), eq(studentId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CourseServiceException.class, () -> progressService.updateLessonProgress(lessonId, studentId, request));
    }
}
