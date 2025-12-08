package com.edubas.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeProblemRequest {
    private String levelType;
    private String moduleName;
    private String lessonTitle;
    private String courseTitle;
    private String username;
}
