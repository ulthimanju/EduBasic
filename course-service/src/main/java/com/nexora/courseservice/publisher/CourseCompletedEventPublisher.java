package com.nexora.courseservice.publisher;

import com.nexora.courseservice.config.KafkaTopics;
import com.nexora.courseservice.constants.LogMessages;
import com.nexora.courseservice.event.CourseCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CourseCompletedEventPublisher {

    private final KafkaTemplate<String, CourseCompletedEvent> kafkaTemplate;
    private final KafkaTopics kafkaTopics;

    public CourseCompletedEventPublisher(
            KafkaTemplate<String, CourseCompletedEvent> kafkaTemplate,
            KafkaTopics kafkaTopics) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopics = kafkaTopics;
    }

    public void publish(CourseCompletedEvent event) {
        try {
            kafkaTemplate.send(
                    kafkaTopics.courseCompleted(),
                    event.studentId().toString(),
                    event
            ).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error(LogMessages.COURSE_COMPLETED_PUBLISH_FAILED,
                            event.studentId(), event.courseId(), ex);
                } else {
                    log.info(LogMessages.COURSE_COMPLETED_PUBLISHED,
                            event.studentId(), event.courseId());
                }
            });
        } catch (Exception e) {
            log.error(LogMessages.COURSE_COMPLETED_PUBLISH_FAILED,
                    event.studentId(), event.courseId(), e);
        }
    }
}
