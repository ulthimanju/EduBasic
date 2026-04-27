package com.app.exam.config;

import com.app.exam.dto.SubmitAttemptEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, SubmitAttemptEvent> submitAttemptConsumerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SubmitAttemptEvent.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SubmitAttemptEvent>
    submitAttemptListenerFactory(
            ConsumerFactory<String, SubmitAttemptEvent> submitAttemptConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, SubmitAttemptEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(submitAttemptConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        return new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate),
                new FixedBackOff(1000L, 3L)
        );
    }

    @Bean
    public NewTopic examSubmittedTopic() {
        return TopicBuilder.name("exam-submitted")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic examSubmittedDltTopic() {
        return TopicBuilder.name("exam-submitted.DLT")
                .partitions(1)
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
    public NewTopic evaluationCompletedDltTopic() {
        return TopicBuilder.name("evaluation-completed.DLT")
                .partitions(1)
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
