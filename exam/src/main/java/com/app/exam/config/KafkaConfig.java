package com.app.exam.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic examSubmittedTopic() {
        return TopicBuilder.name("exam-submitted")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic evaluationCompletedTopic() {
        return TopicBuilder.name("evaluation-completed")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic evaluationNeedsManualTopic() {
        return TopicBuilder.name("evaluation-needs-manual")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
