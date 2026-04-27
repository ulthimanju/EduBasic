package com.nexora.courseservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.dto.response.EnrollmentResponse;
import com.nexora.courseservice.entity.Course;
import com.nexora.courseservice.entity.CourseStatus;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.CourseEnrollmentRepository;
import com.nexora.courseservice.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private UUID studentId;
    private UUID courseId;
    private Course course;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        course = new Course();
        course.setId(courseId);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setTitle("Test Course");
    }

    @Test
    void enroll_ShouldSucceed_WhenValid() {
        when(courseRepository.findByIdAndIsDeletedFalse(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)).thenReturn(false);
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        EnrollmentResponse response = enrollmentService.enroll(courseId, studentId);

        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        verify(enrollmentRepository).save(any());
    }

    @Test
    void enroll_ShouldThrowException_WhenAlreadyEnrolled() {
        when(courseRepository.findByIdAndIsDeletedFalse(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)).thenReturn(true);

        CourseServiceException ex = assertThrows(CourseServiceException.class, 
                () -> enrollmentService.enroll(courseId, studentId));
        assertEquals(ErrorMessages.ALREADY_ENROLLED, ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void enroll_ShouldThrowException_WhenCourseNotPublished() {
        course.setStatus(CourseStatus.DRAFT);
        when(courseRepository.findByIdAndIsDeletedFalse(courseId)).thenReturn(Optional.of(course));

        assertThrows(CourseServiceException.class, () -> enrollmentService.enroll(courseId, studentId));
    }
}
