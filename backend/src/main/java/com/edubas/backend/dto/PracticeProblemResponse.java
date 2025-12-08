package com.edubas.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeProblemResponse {
    private boolean success;
    private String message;
    private String title;
    private String statement;
    private List<String> hints;
    private String inputFormat;
    private String outputFormat;
    private List<TestCase> testCases;
    private String constraints;
}
