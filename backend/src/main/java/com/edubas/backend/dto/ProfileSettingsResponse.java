package com.edubas.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSettingsResponse {
    private String status;
    private String message;
    private Boolean profileVisibility;
    private Boolean emailNotifications;
}
