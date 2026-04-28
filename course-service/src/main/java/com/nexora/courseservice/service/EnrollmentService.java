package com.nexora.courseservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.courseservice.constants.CacheKeys;
import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.constants.LogMessages;
import com.nexora.courseservice.dto.response.EnrollmentResponse;
import com.nexora.courseservice.dto.response.MyCourseSummaryResponse;
import com.nexora.courseservice.entity.Course;
import com.nexora.courseservice.entity.CourseEnrollment;
import com.nexora.courseservice.entity.CourseStatus;
import com.nexora.courseservice.entity.EnrollmentStatus;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.CourseEnrollmentRepository;
import com.nexora.courseservice.repository.CourseRepository;
import com.nexora.courseservice.repository.LessonProgressRepository;
import com.nexora.courseservice.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public EnrollmentResponse enroll(UUID courseId, UUID studentId) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new CourseServiceException(ErrorMessages.ALREADY_ENROLLED, "ALREADY_ENROLLED", HttpStatus.CONFLICT);
        }

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        CourseEnrollment saved = enrollmentRepository.save(enrollment);
        evictEnrollmentCache(studentId, courseId);
        log.info(LogMessages.ENROLLMENT_CREATED, studentId, courseId);
        return mapToResponse(saved, course.getTitle());
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getMyEnrollment(UUID courseId, UUID studentId) {
        String cacheKey = CacheKeys.enrollment(studentId, courseId);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, EnrollmentResponse.class);
            }
        } catch (Exception e) {
            log.warn(LogMessages.CACHE_READ_FAILED, e.getMessage());
        }

        CourseEnrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.ENROLLMENT_NOT_FOUND, "ENROLLMENT_NOT_FOUND", HttpStatus.NOT_FOUND));
        
        Course course = courseRepository.findById(courseId).orElse(null);
        String title = course != null ? course.getTitle() : "Unknown Course";
        
        EnrollmentResponse response = mapToResponse(enrollment, title);
        
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofHours(1));
        } catch (Exception e) {
            log.warn(LogMessages.CACHE_WRITE_FAILED, e.getMessage());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public Page<MyCourseSummaryResponse> getMyEnrolledCourses(UUID studentId, Pageable pageable) {
        return enrollmentRepository.findSummariesByStudentId(studentId, EnrollmentStatus.DROPPED, pageable)
                .map(p -> {
                    String cacheKey = CacheKeys.progress(studentId, p.getCourseId());
                    try {
                        String cached = redisTemplate.opsForValue().get(cacheKey);
                        if (cached != null) {
                            return objectMapper.readValue(cached, MyCourseSummaryResponse.class);
                        }
                    } catch (Exception ex) {
                        log.warn(LogMessages.CACHE_READ_FAILED, ex.getMessage());
                    }

                    MyCourseSummaryResponse summary = new MyCourseSummaryResponse();
                    summary.setCourseId(p.getCourseId());
                    summary.setCourseTitle(p.getCourseTitle());
                    summary.setThumbnailUrl(p.getThumbnailUrl());
                    summary.setEnrollmentStatus(p.getStatus());
                    summary.setEnrolledAt(p.getEnrolledAt());
                    summary.setCompletedAt(p.getCompletedAt());
                    summary.setTotalLessons((int) p.getTotalLessons());
                    summary.setCompletedLessons((int) p.getCompletedLessons());
                    summary.setTotalRequiredExams((int) p.getTotalRequiredExams());
                    summary.setPassedExams(0); // Will be updated in later phases
                    summary.setOverallProgressPercent(p.getTotalLessons() > 0 ? (int) (p.getCompletedLessons() * 100 / p.getTotalLessons()) : 0);
                    
                    try {
                        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(summary), Duration.ofMinutes(30));
                    } catch (Exception ex) {
                        log.warn(LogMessages.CACHE_WRITE_FAILED, ex.getMessage());
                    }
                    return summary;
                });
    }

    @Transactional
    public void dropEnrollment(UUID courseId, UUID studentId) {
        CourseEnrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.ENROLLMENT_NOT_FOUND, "ENROLLMENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new CourseServiceException("Cannot drop a completed course", "INVALID_STATE", HttpStatus.CONFLICT);
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);
        evictEnrollmentCache(studentId, courseId);
        log.info(LogMessages.ENROLLMENT_DROPPED_LOG, studentId, courseId);
    }

    private void evictEnrollmentCache(UUID studentId, UUID courseId) {
        try {
            redisTemplate.delete(CacheKeys.enrollment(studentId, courseId));
            redisTemplate.delete(CacheKeys.progress(studentId, courseId));
        } catch (Exception e) {
            log.warn("Failed to evict enrollment/progress cache for student {} and course {}: {}", studentId, courseId, e.getMessage());
        }
    }

    private EnrollmentResponse mapToResponse(CourseEnrollment e, String title) {
        EnrollmentResponse res = new EnrollmentResponse();
        res.setId(e.getId());
        res.setCourseId(e.getCourseId());
        res.setCourseTitle(title);
        res.setStatus(e.getStatus());
        res.setEnrolledAt(e.getEnrolledAt());
        res.setCompletedAt(e.getCompletedAt());
        return res;
    }
}
