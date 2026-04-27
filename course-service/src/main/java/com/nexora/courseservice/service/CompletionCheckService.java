package com.nexora.courseservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.courseservice.constants.CacheKeys;
import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.constants.LogMessages;
import com.nexora.courseservice.dto.response.CompletionStatusResponse;
import com.nexora.courseservice.dto.response.ExamScoreCache;
import com.nexora.courseservice.entity.*;
import com.nexora.courseservice.event.CourseCompletedEvent;
import com.nexora.courseservice.exception.CourseServiceException;
import com.nexora.courseservice.publisher.CourseCompletedEventPublisher;
import com.nexora.courseservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompletionCheckService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseExamRepository courseExamRepository;
    private final ExamScoreCacheService examScoreCacheService;
    private final CourseCompletionLogRepository completionLogRepository;
    private final CourseCompletedEventPublisher eventPublisher;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void checkAndComplete(UUID courseId, UUID studentId) {

        CourseEnrollment enrollment = enrollmentRepository
                .findByCourseIdAndStudentId(courseId, studentId)
                .orElse(null);

        if (enrollment == null || enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            log.debug("Skipping completion check — no active enrollment for student={} course={}",
                    studentId, courseId);
            return;
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return;

        CompletionRules rules = course.getCompletionRules();

        // Rule 1 — requireAllLessons
        if (rules.requireAllLessons()) {
            long total = lessonRepository.countTotalLessonsForCourse(courseId);
            long completed = lessonProgressRepository
                    .countCompletedLessonsForCourse(courseId, studentId);
            if (completed < total) {
                log.info(LogMessages.COMPLETION_CHECK_FAIL, studentId, courseId,
                        "lessons incomplete: " + completed + "/" + total);
                return;
            }
        }

        // Rule 2 + Rule 3 — requireAllExams + minPassPercent
        List<CourseExam> requiredExams = courseExamRepository
                .findByCourseIdOrderByOrderIndex(courseId)
                .stream()
                .filter(CourseExam::isRequiredToComplete)
                .toList();

        if (rules.requireAllExams() && !requiredExams.isEmpty()) {
            List<Double> scores = new ArrayList<>();

            for (CourseExam courseExam : requiredExams) {
                Optional<ExamScoreCache> score =
                        examScoreCacheService.getExamScore(studentId, courseExam.getExamId());

                if (score.isEmpty() || !score.get().passed()) {
                    log.info(LogMessages.COMPLETION_CHECK_FAIL, studentId, courseId,
                            "exam not passed: " + courseExam.getExamId());
                    return;
                }
                scores.add(score.get().score());
            }

            // Rule 3 — minPassPercent across all required exams
            double avgScore = scores.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            if (avgScore < rules.minPassPercent()) {
                log.info(LogMessages.COMPLETION_CHECK_FAIL, studentId, courseId,
                        "avg score " + avgScore + " below required " + rules.minPassPercent());
                return;
            }
        }

        // All rules passed — mark complete
        log.info(LogMessages.COMPLETION_CHECK_PASS, studentId, courseId);
        markCourseCompleted(enrollment, course, studentId, requiredExams);
    }

    private void markCourseCompleted(
            CourseEnrollment enrollment,
            Course course,
            UUID studentId,
            List<CourseExam> requiredExams) {

        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setCompletedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        List<Map<String, Object>> examScores = requiredExams.stream()
                .map(ce -> {
                    ExamScoreCache score = examScoreCacheService
                            .getExamScore(studentId, ce.getExamId())
                            .orElseThrow();
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("examId", ce.getExamId().toString());
                    entry.put("score", score.score());
                    return entry;
                })
                .toList();

        double avgScore = examScores.isEmpty() ? 100.0 : examScores.stream()
                .mapToDouble(e -> (double) e.get("score"))
                .average()
                .orElse(0.0);

        long totalLessons = lessonRepository.countTotalLessonsForCourse(course.getId());
        long completedLessons = lessonProgressRepository
                .countCompletedLessonsForCourse(course.getId(), studentId);

        CourseCompletionLog completionLog = new CourseCompletionLog();
        completionLog.setCourseId(course.getId());
        completionLog.setStudentId(studentId);
        completionLog.setCompletedAt(LocalDateTime.now());
        completionLog.setTriggerType(TriggerType.AUTO);
        completionLog.setSnapshot(buildSnapshot(completedLessons, totalLessons, examScores, avgScore));
        completionLogRepository.save(completionLog);

        evictProgressCache(studentId, course.getId());
        evictEnrollmentCache(studentId, course.getId());

        log.info(LogMessages.COURSE_COMPLETED, studentId, course.getId());

        eventPublisher.publish(new CourseCompletedEvent(
                course.getId(),
                studentId,
                LocalDateTime.now().toString(),
                "AUTO"
        ));
    }

    private void evictProgressCache(UUID studentId, UUID courseId) {
        try {
            redisTemplate.delete(CacheKeys.progress(studentId, courseId));
        } catch (Exception e) {
            log.warn(LogMessages.CACHE_EVICT_FAILED, CacheKeys.progress(studentId, courseId));
        }
    }

    private void evictEnrollmentCache(UUID studentId, UUID courseId) {
        try {
            redisTemplate.delete(CacheKeys.enrollment(studentId, courseId));
        } catch (Exception e) {
            log.warn(LogMessages.CACHE_EVICT_FAILED, CacheKeys.enrollment(studentId, courseId));
        }
    }

    private String buildSnapshot(
            long completedLessons,
            long totalLessons,
            List<Map<String, Object>> examScores,
            double avgScore) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("completedLessons", completedLessons);
            snapshot.put("totalLessons", totalLessons);
            snapshot.put("examScores", examScores);
            snapshot.put("avgScore", avgScore);
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Transactional(readOnly = true)
    public CompletionStatusResponse getCompletionStatus(UUID courseId, UUID studentId) {
        CourseEnrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.NOT_ENROLLED, "NOT_ENROLLED", HttpStatus.FORBIDDEN));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        CompletionStatusResponse res = new CompletionStatusResponse();
        res.setCourseId(courseId);
        res.setCompleted(enrollment.getStatus() == EnrollmentStatus.COMPLETED);
        res.setRequiredPassPercent(course.getCompletionRules().minPassPercent());

        if (res.isCompleted()) {
            res.setRemainingLessons(Collections.emptyList());
            res.setRemainingExamIds(Collections.emptyList());
            
            completionLogRepository.findByCourseIdAndStudentId(courseId, studentId)
                    .ifPresent(cl -> {
                        try {
                            Map<String, Object> snapshot = objectMapper.readValue(cl.getSnapshot(), Map.class);
                            Object avgScoreObj = snapshot.get("avgScore");
                            if (avgScoreObj instanceof Number num) {
                                res.setCurrentAvgExamScore(num.intValue());
                            }
                        } catch (Exception e) {
                            res.setCurrentAvgExamScore(0);
                        }
                    });
            return res;
        }

        long total = lessonRepository.countTotalLessonsForCourse(courseId);
        long completed = lessonProgressRepository.countCompletedLessonsForCourse(courseId, studentId);
        
        res.setRemainingLessons(Collections.singletonList("Remaining lessons: " + (total - completed)));

        List<CourseExam> requiredExams = courseExamRepository.findByCourseIdOrderByOrderIndex(courseId)
                .stream().filter(CourseExam::isRequiredToComplete).toList();
        
        List<UUID> remainingExamIds = new ArrayList<>();
        double totalScore = 0;
        int scoredExams = 0;

        for (CourseExam ce : requiredExams) {
            Optional<ExamScoreCache> score = examScoreCacheService.getExamScore(studentId, ce.getExamId());
            if (score.isEmpty() || !score.get().passed()) {
                remainingExamIds.add(ce.getExamId());
            }
            if (score.isPresent()) {
                totalScore += score.get().score();
                scoredExams++;
            }
        }

        res.setRemainingExamIds(remainingExamIds);
        res.setCurrentAvgExamScore(scoredExams == 0 ? 0 : (int)(totalScore / scoredExams));

        return res;
    }
}
