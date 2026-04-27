package com.app.sandbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import jakarta.annotation.PostConstruct;
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
    private static final String IMAGE = "openjdk:21-slim";
    private static final String WORKDIR = "/home/sandbox";

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
    }

    @PostConstruct
    public void init() {
        log.info("Pulling Sandbox Image: {}...", IMAGE);
        try {
            dockerClient.pullImageCmd(IMAGE).start().awaitCompletion(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to pull Sandbox Image", e);
        }
    }

    private String createSecureContainer() {
        CreateContainerResponse container = dockerClient.createContainerCmd(IMAGE)
                .withHostConfig(HostConfig.newHostConfig()
                        .withMemory(256 * 1024 * 1024L) // 256MB
                        .withMemorySwap(256 * 1024 * 1024L)
                        .withCpuQuota(50000L) // 50% CPU
                        .withPidsLimit(50L) // Prevent fork bombs
                        .withNetworkMode("none")
                        .withReadonlyRootfs(true)
                        .withSecurityOpts(List.of("no-new-privileges"))
                        .withTmpFs(Map.of(WORKDIR, "size=10m,mode=1777")))
                .withWorkingDir(WORKDIR)
                .withCmd("tail", "-f", "/dev/null")
                .withNetworkDisabled(true)
                .withUser("nobody")
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        return container.getId();
    }

    public List<Map<String, Object>> execute(String language, String sourceCode, JsonNode testCases, int timeLimitMs) {
        if (!"JAVA".equalsIgnoreCase(language)) {
            return List.of(Map.of("status", "ERROR", "actualOutput", "Only Java supported currently"));
        }

        String className = extractClassName(sourceCode);
        String containerId = null;
        try {
            containerId = createSecureContainer();
            return runAllTestCases(containerId, className, sourceCode, testCases, timeLimitMs);
        } catch (Exception e) {
            log.error("Execution failed", e);
            return createErrorResults(testCases, "Internal Error: " + e.getMessage());
        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception e) {
                    log.warn("Failed to remove container: {}", containerId);
                }
            }
        }
    }

    private List<Map<String, Object>> runAllTestCases(String containerId, String className, String sourceCode, JsonNode testCases, int timeLimitMs) throws Exception {
        // 1. Write and Compile
        String encodedSource = Base64.getEncoder().encodeToString(sourceCode.getBytes(StandardCharsets.UTF_8));
        String compileCmd = String.format("echo %s | base64 -d > %s.java && javac %s.java", encodedSource, className, className);
        
        ExecResult compileResult = runExec(containerId, compileCmd, 10000);
        if (compileResult.exitCode != 0) {
            return createErrorResults(testCases, "Compilation Error: " + compileResult.stderr);
        }

        // 2. Run test cases
        List<Map<String, Object>> results = new ArrayList<>();
        boolean timeoutOccurred = false;
        
        for (JsonNode tc : testCases) {
            if (timeoutOccurred) {
                results.add(Map.of(
                    "testCaseId", tc.get("id").asText(),
                    "status", "SKIPPED",
                    "isHidden", tc.has("isHidden") && tc.get("isHidden").asBoolean()
                ));
                continue;
            }

            Map<String, Object> result = runSingleTestCase(containerId, className, tc, timeLimitMs);
            results.add(result);

            if ("TIME_LIMIT_EXCEEDED".equals(result.get("status"))) {
                timeoutOccurred = true;
                log.warn("Test case timed out, skipping remaining tests for container: {}", containerId);
            }
        }
        return results;
    }

    private Map<String, Object> runSingleTestCase(String containerId, String className, JsonNode tc, int timeLimitMs) {
        String testCaseId = tc.get("id").asText();
        String input = tc.has("input") ? tc.get("input").asText() : "";
        String expected = tc.get("expectedOutput").asText().trim();
        boolean isHidden = tc.has("isHidden") && tc.get("isHidden").asBoolean();

        // Safe input passing via base64 to prevent shell injection
        String encodedInput = Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
        String runCmd = String.format("echo %s | base64 -d > input.txt && java %s < input.txt", encodedInput, className);
        
        long start = System.currentTimeMillis();
        
        try {
            ExecResult runResult = runExec(containerId, runCmd, timeLimitMs);
            long executionTime = System.currentTimeMillis() - start;

            if (runResult.timedOut) {
                return Map.of("testCaseId", testCaseId, "status", "TIME_LIMIT_EXCEEDED", "executionTimeMs", (int)executionTime, "isHidden", isHidden);
            }
            if (runResult.exitCode != 0) {
                return Map.of("testCaseId", testCaseId, "status", "RUNTIME_ERROR", "actualOutput", runResult.stderr, "executionTimeMs", (int)executionTime, "isHidden", isHidden);
            }

            String output = runResult.stdout.trim();
            boolean passed = output.equals(expected);
            return Map.of(
                "testCaseId", testCaseId,
                "status", passed ? "PASSED" : "FAILED",
                "actualOutput", output,
                "executionTimeMs", (int)executionTime,
                "isHidden", isHidden
            );
        } catch (Exception e) {
            return Map.of("testCaseId", testCaseId, "status", "ERROR", "actualOutput", e.getMessage(), "isHidden", isHidden);
        }
    }

    private ExecResult runExec(String containerId, String cmd, int timeoutMs) throws Exception {
        ExecCreateCmdResponse execCreate = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("sh", "-c", cmd)
                .exec();

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        
        ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Frame item) {
                if (item.getStreamType() == StreamType.STDOUT) {
                    try { stdout.write(item.getPayload()); } catch (Exception e) {}
                } else if (item.getStreamType() == StreamType.STDERR) {
                    try { stderr.write(item.getPayload()); } catch (Exception e) {}
                }
            }
        };
        dockerClient.execStartCmd(execCreate.getId()).exec(callback);
        
        boolean finished = callback.awaitCompletion(timeoutMs, TimeUnit.MILLISECONDS);
        
        ExecResult result = new ExecResult();
        result.timedOut = !finished;
        result.stdout = stdout.toString(StandardCharsets.UTF_8);
        result.stderr = stderr.toString(StandardCharsets.UTF_8);
        
        if (finished) {
            result.exitCode = dockerClient.inspectExecCmd(execCreate.getId()).exec().getExitCodeLong().intValue();
        }
        return result;
    }

    private static class ExecResult {
        int exitCode;
        String stdout;
        String stderr;
        boolean timedOut;
    }

    private List<Map<String, Object>> createErrorResults(JsonNode testCases, String errorMessage) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (JsonNode tc : testCases) {
            results.add(Map.of(
                "testCaseId", tc.get("id").asText(),
                "status", "ERROR",
                "actualOutput", errorMessage,
                "isHidden", tc.has("isHidden") && tc.get("isHidden").asBoolean()
            ));
        }
        return results;
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
