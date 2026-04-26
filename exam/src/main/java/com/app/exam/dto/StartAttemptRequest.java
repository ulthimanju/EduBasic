package com.app.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class StartAttemptRequest {
    @NotNull
    private UUID examId;
}
