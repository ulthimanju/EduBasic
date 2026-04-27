package com.nexora.courseservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.courseservice.dto.response.ExamScoreCache;
import com.nexora.courseservice.entity.*;
import com.nexora.courseservice.publisher.CourseCompletedEventPublisher;
import com.nexora.courseservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompletionCheckServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private CourseExamRepository courseExamRepository;
    @Mock
    private ExamScoreCacheService examScoreCacheService;
    @Mock
    private CourseCompletionLogRepository completionLogRepository;
    @Mock
    private CourseCompletedEventPublisher eventPublisher;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CompletionCheckService completionCheckService;

    private UUID courseId;
    private UUID studentId;
    private CourseEnrollment enrollment;
    private Course course;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        
        course = new Course();
        course.setId(courseId);
        course.setCompletionRules(new CompletionRules(true, true, 80));
        
        enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
    }

    @Test
    void checkAndComplete_ShouldComplete_WhenAllRulesPass() {
        when(enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrollment));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.countTotalLessonsForCourse(courseId)).thenReturn(5L);
        when(lessonProgressRepository.countCompletedLessonsForCourse(courseId, studentId)).thenReturn(5L);
        
        CourseExam ce = new CourseExam();
        ce.setExamId(UUID.randomUUID());
        ce.setRequiredToComplete(true);
        when(courseExamRepository.findByCourseIdOrderByOrderIndex(courseId)).thenReturn(List.of(ce));
        
        ExamScoreCache score = new ExamScoreCache(85.0, true, LocalDateTime.now());
        when(examScoreCacheService.getExamScore(studentId, ce.getExamId())).thenReturn(Optional.of(score));

        completionCheckService.checkAndComplete(courseId, studentId);

        verify(enrollmentRepository).save(argThat(e -> e.getStatus() == EnrollmentStatus.COMPLETED));
        verify(completionLogRepository).save(any());
        verify(eventPublisher).publish(any());
    }

    @Test
    void checkAndComplete_ShouldNotComplete_WhenExamFails() {
        when(enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrollment));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.countTotalLessonsForCourse(courseId)).thenReturn(5L);
        when(lessonProgressRepository.countCompletedLessonsForCourse(courseId, studentId)).thenReturn(5L);
        
        CourseExam ce = new CourseExam();
        ce.setExamId(UUID.randomUUID());
        ce.setRequiredToComplete(true);
        when(courseExamRepository.findByCourseIdOrderByOrderIndex(courseId)).thenReturn(List.of(ce));
        
        ExamScoreCache score = new ExamScoreCache(50.0, false, LocalDateTime.now());
        when(examScoreCacheService.getExamScore(studentId, ce.getExamId())).thenReturn(Optional.of(score));

        completionCheckService.checkAndComplete(courseId, studentId);

        verify(enrollmentRepository, never()).save(argThat(e -> e.getStatus() == EnrollmentStatus.COMPLETED));
        verify(eventPublisher, never()).publish(any());
    }
}
