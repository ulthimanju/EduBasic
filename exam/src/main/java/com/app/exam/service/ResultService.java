package com.app.exam.service;

import com.app.exam.domain.EvaluationResult;
import com.app.exam.domain.StudentAttempt;
import com.app.exam.dto.ResultResponse;
import com.app.exam.repository.EvaluationResultRepository;
import com.app.exam.repository.StudentAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final EvaluationResultRepository resultRepository;
    private final StudentAttemptRepository attemptRepository;

    @Transactional(readOnly = true)
    public ResultResponse getResultByAttemptId(UUID studentId, UUID attemptId) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new RuntimeException("Unauthorized access to result");
        }

        return resultRepository.findByAttemptId(attemptId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Result not found or pending evaluation"));
    }

    private ResultResponse mapToResponse(EvaluationResult result) {
        ResultResponse response = new ResultResponse();
        response.setId(result.getId());
        response.setAttemptId(result.getAttempt().getId());
        response.setStatus(result.getStatus());
        response.setTotalScore(result.getTotalScore());
        response.setResultJson(result.getResultJson());
        response.setEvaluatedAt(result.getEvaluatedAt());
        return response;
    }
}
