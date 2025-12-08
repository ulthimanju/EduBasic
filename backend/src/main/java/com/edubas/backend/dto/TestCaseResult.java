package com.edubas.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {
    private String input;
    private String expected;
    private String actual;
    private boolean passed;
    private String error;
}
