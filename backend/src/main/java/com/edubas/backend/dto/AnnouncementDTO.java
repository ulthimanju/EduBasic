package com.edubas.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementDTO {
    private String id;
    private String title;
    private String description;
    private String type;
    private Long createdAt;
    private Long updatedAt;
    private String createdByUserId;
    private String createdByUsername;
}
