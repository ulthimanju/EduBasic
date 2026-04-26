package com.app.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class SyncAttemptRequest {
    @NotNull
    private Integer version;

    @NotNull
    private Map<UUID, String> answers; // questionId -> rawAnswer (JSON string or plain text)
}
