package com.app.exam.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CertificateResponse {
    private UUID id;
    private UUID attemptId;
    private String certificateUrl;
    private OffsetDateTime issuedAt;
}
