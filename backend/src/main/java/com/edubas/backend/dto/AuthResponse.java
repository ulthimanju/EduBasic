package com.edubas.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String status;
    private String message;
    private String token;
    private String userId;
    private String username;
    private String email;
    private String role;
    private String avatar; // SVG string for user avatar
    private Boolean profileVisibility;
    private Boolean emailNotifications;
}
