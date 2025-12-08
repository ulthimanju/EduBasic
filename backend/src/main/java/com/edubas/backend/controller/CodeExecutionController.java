package com.edubas.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edubas.backend.dto.CodeExecutionResponse;
import com.edubas.backend.dto.CodeExecutionWithTestsRequest;
import com.edubas.backend.dto.SubmitSolutionRequest;
import com.edubas.backend.dto.SubmitSolutionResponse;
import com.edubas.backend.service.CodeExecutionService;
import com.edubas.backend.service.SolutionService;

@RestController
@RequestMapping("/api/code")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CodeExecutionController {

    @Autowired
    private CodeExecutionService codeExecutionService;

    @Autowired
    private SolutionService solutionService;

    @PostMapping("/execute")
    public CodeExecutionResponse executeCode(@RequestBody CodeExecutionWithTestsRequest request) {
        return codeExecutionService.executeCode(request.getLanguage(), request.getCode(),
                request.getInputs(), request.getExpectedOutputs());
    }

    @PostMapping("/submit")
    public ResponseEntity<SubmitSolutionResponse> submitSolution(
            @RequestBody SubmitSolutionRequest request,
            Authentication authentication) {
        try {
            // Extract username from JWT token
            String username = authentication.getName();

            String solutionId = solutionService.saveSolution(
                    username,
                    request.getProblemId(),
                    request.getCode(),
                    request.getLanguage());

            return ResponseEntity.ok(new SubmitSolutionResponse(solutionId, "Solution submitted successfully!", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new SubmitSolutionResponse(null, e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SubmitSolutionResponse(null, "Failed to submit solution: " + e.getMessage(), false));
        }
    }
}
