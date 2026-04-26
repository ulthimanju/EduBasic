package com.app.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmitAttemptEvent {
    private UUID attemptId;
    private UUID studentId;
    private UUID examId;
}
