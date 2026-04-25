package com.app.auth.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponseDTO {
    private String accessToken;
    private long expiresIn;
}
