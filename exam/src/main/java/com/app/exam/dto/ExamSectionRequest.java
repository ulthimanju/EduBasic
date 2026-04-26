package com.app.exam.dto;

import lombok.Data;

@Data
public class ExamSectionRequest {
    private String title;
    private String description;
    private Integer orderIndex;
}
