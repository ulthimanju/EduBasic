package com.edubas.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeExecutionWithTestsRequest {
    private String language;
    private String code;
    private List<String> inputs;
    private List<String> expectedOutputs;
}
