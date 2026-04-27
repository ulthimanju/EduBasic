package com.nexora.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateModuleRequest {
    @NotBlank(message = "Module title is required")
    private String title;
    private String description;
    @NotNull(message = "Order index is required")
    private Integer orderIndex;
}
