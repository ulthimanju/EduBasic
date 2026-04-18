package com.app.exam.controller;

import com.app.exam.domain.ExamSession;
import com.app.exam.dto.*;
import com.app.exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;

    @PostMapping("/start")
    public ExamSession startExam(@AuthenticationPrincipal UUID userId, @RequestBody StartExamRequest request) {
        return examService.startExam(userId, request);
    }

    @GetMapping("/{sessionId}/question")
    public QuestionResponse getCurrentQuestion(@PathVariable UUID sessionId) {
        return examService.getCurrentQuestion(sessionId);
    }

    @PostMapping("/{sessionId}/answer")
    public AnswerResponse submitAnswer(@PathVariable UUID sessionId, @RequestBody AnswerRequest request) {
        return examService.submitAnswer(sessionId, request);
    }

    @GetMapping("/{sessionId}/result")
    public ExamResultResponse getResult(@PathVariable UUID sessionId) {
        return examService.getResult(sessionId);
    }
}
