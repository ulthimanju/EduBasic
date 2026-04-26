package com.app.exam.controller;

import com.app.exam.domain.Difficulty;
import com.app.exam.domain.QuestionType;
import com.app.exam.dto.CreateQuestionRequest;
import com.app.exam.dto.QuestionResponse;
import com.app.exam.dto.QuestionSummaryResponse;
import com.app.exam.service.QuestionService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/question-bank")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionResponse> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.ok(questionService.createQuestion(request));
    }

    @GetMapping
    @Timed(value = "exam.questions.list", description = "Time taken to list questions from question bank")
    public ResponseEntity<Page<QuestionSummaryResponse>> listQuestions(
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) String tag,
            Pageable pageable) {
        return ResponseEntity.ok(questionService.listQuestions(type, difficulty, createdBy, tag, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestion(@PathVariable UUID id) {
        return ResponseEntity.ok(questionService.getQuestion(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(@PathVariable UUID id, @Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.ok(questionService.updateQuestion(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tags")
    public ResponseEntity<List<String>> listTags() {
        return ResponseEntity.ok(questionService.listTags());
    }

    @PostMapping("/bulk")
    public ResponseEntity<Void> bulkImport(@RequestBody List<CreateQuestionRequest> requests) {
        questionService.bulkImport(requests);
        return ResponseEntity.ok().build();
    }
}
