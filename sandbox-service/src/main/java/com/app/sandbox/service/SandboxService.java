package com.app.sandbox.service;

import com.app.sandbox.domain.CodeSubmission;
import com.app.sandbox.repository.CodeSubmissionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SandboxService {

    private final CodeSubmissionRepository submissionRepository;
    private final DockerExecutor dockerExecutor;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "coding-submitted",
            groupId = "sandbox-group",
            containerFactory = "sandboxListenerFactory"
    )
    @Transactional
    public void consumeSubmission(Map<String, Object> event, Acknowledgment ack) {
        UUID attemptId = UUID.fromString((String) event.get("attemptId"));
        UUID questionId = UUID.fromString((String) event.get("questionId"));
        log.info("Received coding submission for attempt: {}, question: {}", attemptId, questionId);

        try {
            CodeSubmission submission = submissionRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                    .orElseGet(() -> {
                        CodeSubmission s = new CodeSubmission();
                        s.setAttemptId(attemptId);
                        s.setQuestionId(questionId);
                        s.setLanguage((String) event.get("language"));
                        s.setSourceCode((String) event.get("sourceCode"));
                        s.setStatus("QUEUED");
                        return submissionRepository.save(s);
                    });

            if ("COMPLETED".equals(submission.getStatus())) {
                log.info("Submission for attempt {} and question {} already completed. Skipping and acknowledging.", attemptId, questionId);
                publishResult(submission);
                ack.acknowledge();
                return;
            }

            // Handle stale RUNNING state (if service crashed during execution)
            if ("RUNNING".equals(submission.getStatus())) {
                // If it's been running for more than 5 minutes, it's likely stale
                if (submission.getCreatedAt().isBefore(OffsetDateTime.now().minusMinutes(5))) {
                    log.warn("Found stale RUNNING submission for attempt {}. Resuming.", attemptId);
                } else {
                    log.info("Submission for attempt {} is already being processed. Waiting for next retry.", attemptId);
                    return; // Let it retry or finish
                }
            }

            submission.setStatus("RUNNING");
            submission = submissionRepository.save(submission);

            JsonNode testCasesNode = objectMapper.valueToTree(event.get("testCases"));
            int timeLimitMs = ((Number) event.getOrDefault("timeLimitMs", 2000)).intValue();

            List<Map<String, Object>> results = dockerExecutor.execute(
                submission.getLanguage(),
                submission.getSourceCode(),
                testCasesNode,
                timeLimitMs
            );

            long passedCount = results.stream().filter(r -> "PASSED".equals(r.get("status"))).count();
            String overallStatus = passedCount == results.size() ? "PASSED" : (passedCount > 0 ? "PARTIAL" : "FAILED");

            submission.setTestCaseResults(results);
            submission.setOverallStatus(overallStatus);
            submission.setStatus("COMPLETED");
            submission.setCompletedAt(OffsetDateTime.now());
            submissionRepository.save(submission);

            publishResult(submission);
            
            ack.acknowledge();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Duplicate submission detected via unique constraint for attempt: {}. Acknowledging.", attemptId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process coding submission for attempt: {}. Message will be retried or sent to DLT.", attemptId, e);
            throw e; // Rethrow to trigger ErrorHandler
        }
    }

    private void publishResult(CodeSubmission submission) {
        List<Map<String, Object>> results = (List<Map<String, Object>>) submission.getTestCaseResults();
        long passedCount = results.stream().filter(r -> "PASSED".equals(r.get("status"))).count();
        
        Map<String, Object> resultEvent = Map.of(
            "attemptId", submission.getAttemptId().toString(),
            "questionId", submission.getQuestionId().toString(),
            "overallStatus", submission.getOverallStatus(),
            "totalPassed", (int) passedCount,
            "totalTestCases", results.size(),
            "scorePercent", (double) passedCount / results.size() * 100
        );

        kafkaTemplate.send("coding-result", submission.getAttemptId().toString(), resultEvent);
        log.info("Published coding result for attempt: {}, question: {}", submission.getAttemptId(), submission.getQuestionId());
    }
}
