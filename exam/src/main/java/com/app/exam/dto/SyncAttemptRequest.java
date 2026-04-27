package com.app.exam.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class SyncAttemptRequest {
    @NotNull
    @Min(0)
    private Integer version;

    @NotNull
    @Size(max = 100)
    private Map<UUID, @Size(max = 10000) String> answers; // questionId -> rawAnswer (JSON string or plain text)
}
