package com.app.exam.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ExamSectionResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer orderIndex;
    private List<ExamQuestionMappingResponse> questions;
}
