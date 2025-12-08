package com.edubas.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Centralized configuration class for application URLs.
 * This allows environment-based configuration without hardcoding URLs.
 * 
 * Usage:
 * - Dev: Default to localhost (application.properties)
 * - Prod: Set via environment variables (FRONTEND_URL, GEMINI_API_URL)
 */
@Component
public class AppUrlConfig {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.gemini.api.base-url}")
    private String geminiApiBaseUrl;

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public String getGeminiApiBaseUrl() {
        return geminiApiBaseUrl;
    }

    /**
     * Get allowed CORS origin (extracts domain from frontend URL)
     */
    public String getAllowedOrigin() {
        return frontendUrl;
    }
}
