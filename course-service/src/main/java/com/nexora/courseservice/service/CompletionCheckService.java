package com.nexora.courseservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.courseservice.constants.CacheKeys;
import com.nexora.courseservice.constants.ErrorMessages;
import com.nexora.courseservice.constants.LogMessages;
import com.nexora.courseservice.dto.response.CompletionStatusResponse;
import com.nexora.courseservice.dto.response.ExamScoreCache;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompletionCheckService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseCompletionLogRepository completionLogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void checkAndComplete(UUID courseId, UUID studentId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return;

        CourseEnrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .orElse(null);
        if (enrollment == null) return;

        CompletionRules rules = course.getCompletionRules();
        
        // Rule 1: All Lessons
        long totalLessons = lessonRepository.countTotalLessonsForCourse(courseId);
        long completedLessons = progressRepository.countCompletedLessonsForCourse(courseId, studentId);
        boolean lessonsDone = !rules.requireAllLessons() || (completedLessons >= totalLessons);

        // Rule 2 & 3: Exams
        List<CourseExam> requiredExams = course.getCourseExams().stream()
                .filter(CourseExam::isRequiredToComplete)
                .collect(Collectors.toList());
        
        List<ExamScoreCache> scores = fetchExamScores(studentId, requiredExams);
        boolean examsPassed = !rules.requireAllExams() || (scores.size() >= requiredExams.size() && scores.stream().allMatch(ExamScoreCache::passed));
        
        double avgScore = scores.isEmpty() ? 0 : scores.stream().mapToDouble(ExamScoreCache::score).average().orElse(0);
        boolean scoreThresholdMet = avgScore >= rules.minPassPercent();

        if (lessonsDone && examsPassed && scoreThresholdMet) {
            completeCourse(course, enrollment, studentId, (int)totalLessons, (int)completedLessons, scores, avgScore);
        }
    }

    @Transactional(readOnly = true)
    public CompletionStatusResponse getCompletionStatus(UUID courseId, UUID studentId) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.COURSE_NOT_FOUND, "COURSE_NOT_FOUND", HttpStatus.NOT_FOUND));

        CourseEnrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new CourseServiceException(ErrorMessages.NOT_ENROLLED, "NOT_ENROLLED", HttpStatus.FORBIDDEN));

        CompletionStatusResponse res = new CompletionStatusResponse();
        res.setCourseId(courseId);
        res.setCompleted(enrollment.getStatus() == EnrollmentStatus.COMPLETED);
        res.setRequiredPassPercent(course.getCompletionRules().minPassPercent());

        if (res.isCompleted()) {
            res.setRemainingLessons(Collections.emptyList());
            res.setRemainingExamIds(Collections.emptyList());
            // Need to fetch score from log snapshot in real app
            res.setCurrentAvgExamScore(100); 
            return res;
        }

        // Fetch remaining lessons
        // Simplified: list of all lesson titles for now
        res.setRemainingLessons(course.getModules().stream()
                .filter(m -> !m.isDeleted())
                .flatMap(m -> m.getLessons().stream())
                .filter(l -> !l.isDeleted())
                .map(Lesson::getTitle)
                .collect(Collectors.toList()));

        // Fetch remaining exams
        List<CourseExam> requiredExams = course.getCourseExams().stream()
                .filter(CourseExam::isRequiredToComplete)
                .collect(Collectors.toList());
        List<ExamScoreCache> scores = fetchExamScores(studentId, requiredExams);
        Set<UUID> passedExamIds = scores.stream()
                .filter(ExamScoreCache::passed)
                .map(s -> UUID.randomUUID()) // Placeholder as ExamScoreCache doesn't have examId
                .collect(Collectors.toSet());
        
        res.setRemainingExamIds(requiredExams.stream()
                .map(CourseExam::getExamId)
                .filter(id -> !passedExamIds.contains(id))
                .collect(Collectors.toList()));

        res.setCurrentAvgExamScore((int) (scores.isEmpty() ? 0 : scores.stream().mapToDouble(ExamScoreCache::score).average().orElse(0)));

        return res;
    }

    private List<ExamScoreCache> fetchExamScores(UUID studentId, List<CourseExam> exams) {
        List<ExamScoreCache> scores = new ArrayList<>();
        for (CourseExam exam : exams) {
            try {
                String key = CacheKeys.examScore(studentId, exam.getExamId());
                String cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    scores.add(objectMapper.readValue(cached, ExamScoreCache.class));
                }
            } catch (Exception e) {
                log.warn(LogMessages.CACHE_READ_FAILED, e.getMessage());
            }
        }
        return scores;
    }

    private void completeCourse(Course course, CourseEnrollment enrollment, UUID studentId, int total, int done, List<ExamScoreCache> scores, double avg) {
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setCompletedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        CourseCompletionLog logEntry = new CourseCompletionLog();
        logEntry.setCourseId(course.getId());
        logEntry.setStudentId(studentId);
        logEntry.setTriggerType("AUTO");
        
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("completedLessons", done);
            snapshot.put("totalLessons", total);
            snapshot.put("avgScore", avg);
            snapshot.put("examScores", scores);
            logEntry.setSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            logEntry.setSnapshot("{}");
        }

        completionLogRepository.save(logEntry);
        
        // Evict caches
        redisTemplate.delete(CacheKeys.enrollment(studentId, course.getId()));
        redisTemplate.delete(CacheKeys.progress(studentId, course.getId()));
        
        log.info(LogMessages.COURSE_COMPLETED, studentId, course.getId());
        // TODO: Publish course-completed Kafka event in Phase 5
    }
}
