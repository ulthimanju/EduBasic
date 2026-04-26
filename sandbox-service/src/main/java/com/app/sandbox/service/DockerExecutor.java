package com.app.sandbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DockerExecutor {

    private final DockerClient dockerClient;

    public DockerExecutor() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
        
        // Ensure the sandbox image is pulled
        try {
            log.info("Pulling openjdk:21-slim image...");
            dockerClient.pullImageCmd("openjdk:21-slim")
                    .start()
                    .awaitCompletion(5, TimeUnit.MINUTES);
            log.info("Successfully pulled openjdk:21-slim");
        } catch (Exception e) {
            log.warn("Failed to pull openjdk:21-slim. If it's not present locally, grading will fail.", e);
        }
    }
    public List<Map<String, Object>> execute(String language, String sourceCode, JsonNode testCases, int timeLimitMs) {
        if (!"JAVA".equalsIgnoreCase(language)) {
            return List.of(Map.of("status", "ERROR", "actualOutput", "Only Java supported currently"));
        }

        String className = extractClassName(sourceCode);
        List<Map<String, Object>> results = new ArrayList<>();

        for (JsonNode tc : testCases) {
            results.add(runInContainer(className, sourceCode, tc, timeLimitMs));
        }

        return results;
    }

    private Map<String, Object> runInContainer(String className, String sourceCode, JsonNode tc, int timeLimitMs) {
        String testCaseId = tc.get("id").asText();
        String input = tc.has("input") ? tc.get("input").asText() : "";
        String expected = tc.get("expectedOutput").asText().trim();
        boolean isHidden = tc.has("isHidden") && tc.get("isHidden").asBoolean();

        String containerId = null;
        try {
            // Encode source to avoid shell injection issues in a simple command
            String encodedSource = Base64.getEncoder().encodeToString(sourceCode.getBytes(StandardCharsets.UTF_8));
            
            // Script to decode, compile, and run with input
            String cmd = String.format(
                "echo %s | base64 -d > %s.java && javac %s.java && echo -n \"%s\" | java %s",
                encodedSource, className, className, input.replace("\"", "\\\""), className
            );

            CreateContainerResponse container = dockerClient.createContainerCmd("openjdk:21-slim")
                    .withHostConfig(HostConfig.newHostConfig()
                            .withMemory(256 * 1024 * 1024L) // 256MB
                            .withMemorySwap(256 * 1024 * 1024L)
                            .withCpuQuota(50000L) // 0.5 CPU
                            .withNetworkMode("none"))
                    .withName("sandbox-" + testCaseId + "-" + System.currentTimeMillis())
                    .withCmd("sh", "-c", cmd)
                    .withNetworkDisabled(true)
                    .exec();

            containerId = container.getId();
            long start = System.currentTimeMillis();
            dockerClient.startContainerCmd(containerId).exec();

            // Wait for completion
            WaitContainerResultCallback callback = new WaitContainerResultCallback();
            dockerClient.waitContainerCmd(containerId).exec(callback);
            
            boolean finished = callback.awaitCompletion(timeLimitMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - start;

            if (!finished) {
                try { dockerClient.stopContainerCmd(containerId).exec(); } catch (Exception e) {}
                return Map.of("testCaseId", testCaseId, "status", "TIME_LIMIT_EXCEEDED", "executionTimeMs", (int)executionTime, "isHidden", isHidden);
            }

            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
            
            // Get logs
            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
            
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<>() {
                        @Override
                        public void onNext(com.github.dockerjava.api.model.Frame item) {
                            if (item.getStreamType() == com.github.dockerjava.api.model.StreamType.STDOUT) {
                                try { stdoutStream.write(item.getPayload()); } catch (Exception e) {}
                            } else {
                                try { stderrStream.write(item.getPayload()); } catch (Exception e) {}
                            }
                        }
                    }).awaitCompletion(5, TimeUnit.SECONDS);

            String output = stdoutStream.toString().trim();
            String error = stderrStream.toString().trim();

            if (inspect.getState().getExitCodeLong() != 0) {
                return Map.of("testCaseId", testCaseId, "status", "RUNTIME_ERROR", "actualOutput", error, "executionTimeMs", (int)executionTime, "isHidden", isHidden);
            }

            boolean passed = output.equals(expected);
            return Map.of(
                "testCaseId", testCaseId,
                "status", passed ? "PASSED" : "FAILED",
                "actualOutput", output,
                "executionTimeMs", (int)executionTime,
                "isHidden", isHidden
            );

        } catch (Exception e) {
            log.error("Docker execution failed", e);
            return Map.of("testCaseId", testCaseId, "status", "ERROR", "actualOutput", e.getMessage(), "isHidden", isHidden);
        } finally {
            if (containerId != null) {
                try { dockerClient.removeContainerCmd(containerId).withForce(true).exec(); } catch (Exception e) {}
            }
        }
    }

    private String extractClassName(String sourceCode) {
        Pattern pattern = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(sourceCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Main";
    }
}
