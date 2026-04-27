package com.app.sandbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DockerExecutorTest {

    @Mock
    private DockerClient dockerClient;

    @InjectMocks
    private DockerExecutor dockerExecutor;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // DockerExecutor creates its own client in constructor, so we inject the mock
        ReflectionTestUtils.setField(dockerExecutor, "dockerClient", dockerClient);
    }

    @Test
    void testExecute_Java_Success() throws Exception {
        String sourceCode = "public class Main { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        ArrayNode testCases = objectMapper.createArrayNode();
        testCases.addObject()
                .put("id", "tc1")
                .put("input", "")
                .put("expectedOutput", "Hello");

        // Mock container creation
        CreateContainerCmd createCmd = mock(CreateContainerCmd.class);
        CreateContainerResponse createResponse = new CreateContainerResponse();
        ReflectionTestUtils.setField(createResponse, "id", "container-id");
        when(dockerClient.createContainerCmd(anyString())).thenReturn(createCmd);
        when(createCmd.withHostConfig(any())).thenReturn(createCmd);
        when(createCmd.withWorkingDir(anyString())).thenReturn(createCmd);
        when(createCmd.withCmd(anyString(), anyString(), anyString())).thenReturn(createCmd);
        when(createCmd.withNetworkDisabled(anyBoolean())).thenReturn(createCmd);
        when(createCmd.withUser(anyString())).thenReturn(createCmd);
        when(createCmd.exec()).thenReturn(createResponse);

        // Mock container start
        StartContainerCmd startCmd = mock(StartContainerCmd.class);
        when(dockerClient.startContainerCmd(anyString())).thenReturn(startCmd);

        // Mock Exec Create for compilation
        ExecCreateCmd execCreateCmd = mock(ExecCreateCmd.class);
        ExecCreateCmdResponse execCreateResponse = new ExecCreateCmdResponse();
        ReflectionTestUtils.setField(execCreateResponse, "id", "exec-compile-id");
        when(dockerClient.execCreateCmd(anyString())).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStdout(anyBoolean())).thenReturn(execCreateCmd);
        when(execCreateCmd.withAttachStderr(anyBoolean())).thenReturn(execCreateCmd);
        when(execCreateCmd.withCmd(any(String[].class))).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);

        // Mock Exec Start
        ExecStartCmd execStartCmd = mock(ExecStartCmd.class);
        when(dockerClient.execStartCmd(anyString())).thenReturn(execStartCmd);

        // Mock Inspect Exec for exit code 0
        InspectExecCmd inspectExecCmd = mock(InspectExecCmd.class);
        InspectExecResponse inspectExecResponse = mock(InspectExecResponse.class);
        when(dockerClient.inspectExecCmd(anyString())).thenReturn(inspectExecCmd);
        when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);
        when(inspectExecResponse.getExitCodeLong()).thenReturn(0L);

        // Mock container removal
        RemoveContainerCmd removeCmd = mock(RemoveContainerCmd.class);
        when(dockerClient.removeContainerCmd(anyString())).thenReturn(removeCmd);
        when(removeCmd.withForce(anyBoolean())).thenReturn(removeCmd);

        // Mock the async callback for compilation (simplified)
        doAnswer(invocation -> {
            com.github.dockerjava.api.async.ResultCallback.Adapter<Frame> callback = invocation.getArgument(0);
            callback.onComplete();
            return null;
        }).when(execStartCmd).exec(any());

        // For execution of test case, we need to provide output "Hello"
        doAnswer(invocation -> {
            com.github.dockerjava.api.async.ResultCallback.Adapter<Frame> callback = invocation.getArgument(0);
            callback.onNext(new Frame(StreamType.STDOUT, "Hello\n".getBytes()));
            callback.onComplete();
            return null;
        }).when(execStartCmd).exec(any());

        List<Map<String, Object>> results = dockerExecutor.execute("JAVA", sourceCode, testCases, 2000);

        assertEquals(1, results.size());
        assertEquals("PASSED", results.get(0).get("status"));
        assertEquals("Hello", results.get(0).get("actualOutput"));
    }
}
