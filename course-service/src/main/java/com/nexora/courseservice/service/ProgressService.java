package com.nexora.courseservice.service;

import com.nexora.courseservice.constants.CacheKeys;
import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.constants.LogMessages;
import com.nexora.courseservice.dto.request.UpdateProgressRequest;
import com.nexora.courseservice.dto.response.*;
import com.nexora.courseservice.entity.*;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final LessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final CompletionCheckService completionCheckService;

    @Transactional
    public LessonProgressResponse updateLessonProgress(UUID lessonId, UUID studentId, UpdateProgressRequest request) {
        Lesson lesson = lessonRepository.findByIdAndIsDeletedFalse(lessonId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.LESSON_NOT_FOUND, "LESSON_NOT_FOUND", HttpStatus.NOT_FOUND));

        UUID courseId = lesson.getModule().getCourse().getId();
        CourseEnrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.NOT_ENROLLED, "NOT_ENROLLED", HttpStatus.FORBIDDEN));

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new CourseServiceException(ErrorMessages.ENROLLMENT_DROPPED, "ENROLLMENT_DROPPED", HttpStatus.FORBIDDEN);
        }

        LessonProgress progress = progressRepository.findByLessonIdAndStudentId(lessonId, studentId)
                .orElseGet(() -> {
                    LessonProgress lp = new LessonProgress();
                    lp.setLessonId(lessonId);
                    lp.setStudentId(studentId);
                    return lp;
                });

        // Never allow progress to go backwards
        if (request.getProgressPercent() > progress.getProgressPercent()) {
            progress.setProgressPercent(request.getProgressPercent());
            
            // Derive status
            if (progress.getProgressPercent() == 100) {
                progress.setStatus(ProgressStatus.COMPLETED);
                progress.setCompletedAt(LocalDateTime.now());
            } else if (progress.getProgressPercent() > 0) {
                progress.setStatus(ProgressStatus.IN_PROGRESS);
            } else {
                progress.setStatus(ProgressStatus.NOT_STARTED);
            }
            
            progressRepository.save(progress);
            log.info(LogMessages.PROGRESS_UPDATED_LOG, studentId, lessonId, progress.getProgressPercent());
            
            // Evict cache
            redisTemplate.delete(CacheKeys.progress(studentId, courseId));
            
            // Trigger completion check
            completionCheckService.checkAndComplete(courseId, studentId);
        }

        return mapToProgressResponse(progress);
    }

    @Transactional(readOnly = true)
    public LessonProgressResponse getLessonProgress(UUID lessonId, UUID studentId) {
        return progressRepository.findByLessonIdAndStudentId(lessonId, studentId)
                .map(this::mapToProgressResponse)
                .orElseGet(() -> {
                    LessonProgressResponse res = new LessonProgressResponse();
                    res.setLessonId(lessonId);
                    res.setStatus(ProgressStatus.NOT_STARTED);
                    res.setProgressPercent(0);
                    return res;
                });
    }

    @Transactional(readOnly = true)
    public CourseOutlineResponse getCourseOutlineWithProgress(UUID courseId, UUID studentId) {
        if (!enrollmentRepository.existsByCourseIdAndStudentIdAndStatusNot(courseId, studentId, EnrollmentStatus.DROPPED)) {
            throw new CourseServiceException(ErrorMessages.NOT_ENROLLED, "NOT_ENROLLED", HttpStatus.FORBIDDEN);
        }

        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        CourseOutlineResponse res = new CourseOutlineResponse();
        res.setId(course.getId());
        res.setTitle(course.getTitle());
        res.setDescription(course.getDescription());
        res.setThumbnailUrl(course.getThumbnailUrl());
        res.setCompletionRules(course.getCompletionRules());
        
        res.setModules(course.getModules().stream()
                .filter(m -> !m.isDeleted())
                .map(m -> {
                    ModuleOutlineResponse mr = new ModuleOutlineResponse();
                    mr.setId(m.getId());
                    mr.setTitle(m.getTitle());
                    mr.setDescription(m.getDescription());
                    mr.setOrderIndex(m.getOrderIndex());
                    mr.setLessons(m.getLessons().stream()
                            .filter(l -> !l.isDeleted())
                            .map(l -> {
                                LessonOutlineResponse lr = new LessonOutlineResponse();
                                lr.setId(l.getId());
                                lr.setTitle(l.getTitle());
                                lr.setContentType(l.getContentType());
                                lr.setDurationMinutes(l.getDurationMinutes());
                                lr.setOrderIndex(l.getOrderIndex());
                                lr.setPreview(l.isPreview());
                                lr.setContentBody(l.getContentBody()); // Enrolled student sees all
                                lr.setContentUrl(l.getContentUrl());
                                
                                lr.setProgress(getLessonProgress(l.getId(), studentId));
                                return lr;
                            }).collect(Collectors.toList()));
                    return mr;
                }).collect(Collectors.toList()));
        
        res.setExams(course.getCourseExams().stream()
                .map(e -> {
                    CourseExamResponse er = new CourseExamResponse();
                    er.setId(e.getId());
                    er.setExamId(e.getExamId());
                    er.setTitle(e.getTitle());
                    er.setOrderIndex(e.getOrderIndex());
                    er.setRequiredToComplete(e.isRequiredToComplete());
                    er.setMinPassPercent(e.getMinPassPercent());
                    return er;
                }).collect(Collectors.toList()));

        return res;
    }

    private LessonProgressResponse mapToProgressResponse(LessonProgress lp) {
        LessonProgressResponse res = new LessonProgressResponse();
        res.setLessonId(lp.getLessonId());
        res.setStatus(lp.getStatus());
        res.setProgressPercent(lp.getProgressPercent());
        res.setLastAccessedAt(lp.getLastAccessedAt());
        res.setCompletedAt(lp.getCompletedAt());
        return res;
    }
}
