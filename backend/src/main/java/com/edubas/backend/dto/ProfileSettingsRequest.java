package com.edubas.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSettingsRequest {
    private String userId;
    private Boolean profileVisibility;
    private Boolean emailNotifications;
}
