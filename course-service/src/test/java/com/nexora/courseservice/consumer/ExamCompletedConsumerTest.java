package com.nexora.courseservice.consumer;

import com.nexora.courseservice.dto.response.ExamScoreCache;
import com.nexora.courseservice.entity.Course;
import com.nexora.courseservice.entity.CourseExam;
import com.nexora.courseservice.event.ExamCompletedEvent;
import com.nexora.courseservice.repository.CourseExamRepository;
import com.nexora.courseservice.service.CompletionCheckService;
import com.nexora.courseservice.service.ExamScoreCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamCompletedConsumerTest {

    @Mock
    private ExamScoreCacheService examScoreCacheService;
    @Mock
    private CourseExamRepository courseExamRepository;
    @Mock
    private CompletionCheckService completionCheckService;
    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private ExamCompletedConsumer examCompletedConsumer;

    private ExamCompletedEvent event;

    @BeforeEach
    void setUp() {
        event = new ExamCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                85.0,
                true,
                LocalDateTime.now().toString()
        );
    }

    @Test
    void consume_ShouldProcessEventAndAcknowledge() {
        CourseExam courseExam = new CourseExam();
        Course course = new Course();
        course.setId(UUID.randomUUID());
        courseExam.setCourse(course);

        when(examScoreCacheService.getExamScore(any(), any())).thenReturn(Optional.empty());
        when(courseExamRepository.findByExamId(event.examId())).thenReturn(List.of(courseExam));

        examCompletedConsumer.consume(event, "topic", 0, 0, acknowledgment);

        verify(examScoreCacheService).saveExamScore(eq(event.studentId()), eq(event.examId()), any());
        verify(completionCheckService).checkAndComplete(eq(course.getId()), eq(event.studentId()));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consume_ShouldSkipDuplicatePassingEvent() {
        ExamScoreCache existing = new ExamScoreCache(90.0, true, LocalDateTime.now());
        when(examScoreCacheService.getExamScore(event.studentId(), event.examId()))
                .thenReturn(Optional.of(existing));

        // Even if new event failed, we skip because we already have a passing score
        ExamCompletedEvent failedEvent = new ExamCompletedEvent(
                event.attemptId(), event.examId(), event.studentId(), 40.0, false, event.completedAt());

        examCompletedConsumer.consume(failedEvent, "topic", 0, 0, acknowledgment);

        verify(examScoreCacheService, never()).saveExamScore(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }
}
