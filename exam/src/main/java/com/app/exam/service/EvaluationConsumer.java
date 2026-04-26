package com.app.exam.service;

import com.app.exam.dto.SubmitAttemptEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationConsumer {

    private final EvaluationService evaluationService;
    private final CertificateService certificateService;

    @KafkaListener(topics = "exam-submitted", groupId = "evaluation-group")
    public void consumeSubmission(SubmitAttemptEvent event) {
        log.info("Received exam submission for evaluation: {}", event.getAttemptId());
        evaluationService.evaluateAttempt(event.getAttemptId());
    }

    @KafkaListener(topics = "evaluation-completed", groupId = "certificate-group")
    public void consumeCompletion(SubmitAttemptEvent event) {
        log.info("Received evaluation completion for certificate generation: {}", event.getAttemptId());
        certificateService.generateCertificate(event.getAttemptId());
    }
}
