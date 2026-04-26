package com.app.exam.dto;

import com.app.exam.domain.ExamStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class ExamSummaryResponse {
    private UUID id;
    private UUID createdBy;
    private String title;
    private ExamStatus status;
}
