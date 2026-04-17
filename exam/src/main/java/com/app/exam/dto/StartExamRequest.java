package com.app.exam.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class StartExamRequest {
    private UUID courseId;
}
