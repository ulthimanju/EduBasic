package com.nexora.courseservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopics {

    @Value("${kafka.topics.exam-completed}")
    private String examCompleted;

    @Value("${kafka.topics.course-completed}")
    private String courseCompleted;

    public String examCompleted() { return examCompleted; }
    public String courseCompleted() { return courseCompleted; }
}
