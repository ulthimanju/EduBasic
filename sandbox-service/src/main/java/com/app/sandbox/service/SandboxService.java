package com.app.sandbox.service;

import com.app.sandbox.domain.CodeSubmission;
import com.app.sandbox.repository.CodeSubmissionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
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

    @KafkaListener(topics = "coding-submitted", groupId = "sandbox-group")
    @Transactional
    public void consumeSubmission(Map<String, Object> event) {
        log.info("Received coding submission for attempt: {}", event.get("attemptId"));

        CodeSubmission submission = new CodeSubmission();
        submission.setAttemptId(UUID.fromString((String) event.get("attemptId")));
        submission.setQuestionId(UUID.fromString((String) event.get("questionId")));
        submission.setLanguage((String) event.get("language"));
        submission.setSourceCode((String) event.get("sourceCode"));
        submission.setStatus("RUNNING");
        submission = submissionRepository.save(submission);

        List<Map<String, Object>> results = dockerExecutor.execute(
            submission.getLanguage(),
            submission.getSourceCode(),
            (JsonNode) event.get("testCases"),
            (Integer) event.get("timeLimitMs")
        );

        long passedCount = results.stream().filter(r -> "PASSED".equals(r.get("status"))).count();
        String overallStatus = passedCount == results.size() ? "PASSED" : (passedCount > 0 ? "PARTIAL" : "FAILED");

        submission.setTestCaseResults(results);
        submission.setOverallStatus(overallStatus);
        submission.setStatus("COMPLETED");
        submission.setCompletedAt(OffsetDateTime.now());
        submissionRepository.save(submission);

        // Produce result back to Evaluation Engine
        Map<String, Object> resultEvent = Map.of(
            "attemptId", submission.getAttemptId().toString(),
            "questionId", submission.getQuestionId().toString(),
            "overallStatus", overallStatus,
            "totalPassed", (int) passedCount,
            "totalTestCases", results.size(),
            "scorePercent", (double) passedCount / results.size() * 100
        );

        kafkaTemplate.send("coding-result", submission.getAttemptId().toString(), resultEvent);
        log.info("Published coding result for attempt: {}", submission.getAttemptId());
    }
}
