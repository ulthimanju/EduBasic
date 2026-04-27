package com.nexora.courseservice.consumer;

import com.nexora.courseservice.constants.LogMessages;
import com.nexora.courseservice.dto.response.ExamScoreCache;
import com.nexora.courseservice.entity.CourseExam;
import com.nexora.courseservice.event.ExamCompletedEvent;
import com.nexora.courseservice.repository.CourseExamRepository;
import com.nexora.courseservice.service.CompletionCheckService;
import com.nexora.courseservice.service.ExamScoreCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ExamCompletedConsumer {

    private final ExamScoreCacheService examScoreCacheService;
    private final CourseExamRepository courseExamRepository;
    private final CompletionCheckService completionCheckService;

    public ExamCompletedConsumer(
            ExamScoreCacheService examScoreCacheService,
            CourseExamRepository courseExamRepository,
            CompletionCheckService completionCheckService) {
        this.examScoreCacheService = examScoreCacheService;
        this.courseExamRepository = courseExamRepository;
        this.completionCheckService = completionCheckService;
    }

    @KafkaListener(
        topics = "${kafka.topics.exam-completed}",
        groupId = "course-service-group",
        containerFactory = "examCompletedListenerFactory"
    )
    public void consume(
            ExamCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info(LogMessages.EXAM_COMPLETED_PROCESSING,
                event.studentId(), event.examId(), event.score());

        try {
            processExamCompleted(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error(LogMessages.KAFKA_CONSUMER_ERROR, topic, partition, offset, e);
            throw e;
        }
    }

    private void processExamCompleted(ExamCompletedEvent event) {
        Optional<ExamScoreCache> existing =
                examScoreCacheService.getExamScore(event.studentId(), event.examId());
        
        if (existing.isPresent() && existing.get().passed() && !event.passed()) {
            log.info(LogMessages.EXAM_COMPLETED_DUPLICATE,
                    event.studentId(), event.examId());
            return;
        }

        ExamScoreCache scoreCache = new ExamScoreCache(
                event.score(),
                event.passed(),
                LocalDateTime.parse(event.completedAt())
        );
        examScoreCacheService.saveExamScore(event.studentId(), event.examId(), scoreCache);

        List<CourseExam> linkedCourses =
                courseExamRepository.findByExamId(event.examId());

        if (linkedCourses.isEmpty()) {
            log.debug("No courses linked to examId={} — skipping completion check",
                    event.examId());
            return;
        }

        for (CourseExam courseExam : linkedCourses) {
            try {
                completionCheckService.checkAndComplete(
                        courseExam.getCourse().getId(),
                        event.studentId()
                );
            } catch (Exception e) {
                log.error(LogMessages.COMPLETION_CHECK_FAIL,
                        event.studentId(),
                        courseExam.getCourse().getId(),
                        e.getMessage());
            }
        }
    }
}
