package com.nexora.courseservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateProgressRequest {
    @NotNull(message = "Progress percent is required")
    @Min(0)
    @Max(100)
    private Integer progressPercent;
}
