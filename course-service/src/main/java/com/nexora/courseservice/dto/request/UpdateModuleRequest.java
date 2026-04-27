package com.nexora.courseservice.dto.request;

import lombok.Data;

@Data
public class UpdateModuleRequest {
    private String title;
    private String description;
    private Integer orderIndex;
}
