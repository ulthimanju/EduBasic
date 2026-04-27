package com.nexora.courseservice.publisher;

import com.nexora.courseservice.config.KafkaTopics;
import com.nexora.courseservice.event.CourseCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseCompletedEventPublisherTest {

    @Mock
    private KafkaTemplate<String, CourseCompletedEvent> kafkaTemplate;
    @Mock
    private KafkaTopics kafkaTopics;

    @InjectMocks
    private CourseCompletedEventPublisher publisher;

    @BeforeEach
    void setUp() {
        when(kafkaTopics.courseCompleted()).thenReturn("course-completed");
    }

    @Test
    void publish_ShouldCallKafkaTemplate() {
        CourseCompletedEvent event = new CourseCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now().toString(),
                "AUTO"
        );

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        publisher.publish(event);

        verify(kafkaTemplate).send(eq("course-completed"), eq(event.studentId().toString()), eq(event));
    }
}
