package com.app.exam.service;

import com.app.exam.dto.SubmitAttemptEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationConsumer {

    private final EvaluationService evaluationService;
    private final CertificateService certificateService;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = "exam-submitted",
            groupId = "evaluation-group",
            containerFactory = "submitAttemptListenerFactory"
    )
    public void consumeSubmission(SubmitAttemptEvent event, Acknowledgment ack) {
        log.info("Received exam submission for evaluation: {}", event.getAttemptId());
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            evaluationService.evaluateAttempt(event.getAttemptId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to evaluate attempt: {}. Message will be retried or sent to DLT.", event.getAttemptId(), e);
            throw e; // Rethrow to trigger ErrorHandler
        } finally {
            sample.stop(meterRegistry.timer("exam.evaluation.time"));
        }
    }

    @KafkaListener(
            topics = "evaluation-completed",
            groupId = "certificate-group",
            containerFactory = "submitAttemptListenerFactory"
    )
    public void consumeCompletion(SubmitAttemptEvent event, Acknowledgment ack) {
        log.info("Received evaluation completion for certificate generation: {}", event.getAttemptId());
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            certificateService.generateCertificate(event.getAttemptId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to generate certificate for attempt: {}. Message will be retried or sent to DLT.", event.getAttemptId(), e);
            throw e; // Rethrow to trigger ErrorHandler
        } finally {
            sample.stop(meterRegistry.timer("exam.certificate.generation.time"));
        }
    }
}
