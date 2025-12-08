package com.edubas.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeExecutionResponse {
    private List<TestCaseResult> results;
    private int passedCount;
    private int totalCount;
}
