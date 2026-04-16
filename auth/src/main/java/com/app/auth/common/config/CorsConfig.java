package com.app.auth.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration.
 *
 * <p>Allows requests from the exact frontend origin only — never {@code *}.
 * {@code allowCredentials(true)} is required for the browser to send the
 * HttpOnly auth cookie on cross-origin API calls.</p>
 *
 * <p>Allowed methods: GET, POST, OPTIONS (preflight).</p>
 */
@Configuration
public class CorsConfig {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Exact origin only — no wildcard
        config.setAllowedOrigins(List.of(frontendUrl));

        // Required for cookies to be sent cross-origin
        config.setAllowCredentials(true);

        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));

        // Allow standard headers + Authorization (future-proofing)
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With"));

        // Cache preflight for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
