package com.app.auth.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Binds {@code app.security.admin.*} configuration.
 *
 * <p>Parses the comma-separated {@code allowed-emails} string by trimming,
 * lowercasing, and dropping blank tokens. An empty or missing env var yields
 * an empty set — all management routes return {@code 403} (deny-by-default).</p>
 *
 * <p>Activated by {@code @EnableConfigurationProperties(AdminProperties.class)}
 * in {@link SecurityConfig}.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.admin")
public class AdminProperties {

    /**
     * Raw comma-separated string from {@code ADMIN_ALLOWED_EMAILS} env var.
     * Spring binds this field automatically via the prefix.
     */
    private String allowedEmails = "";

    // ── Parsed view ──────────────────────────────────────────────────────────

    /**
     * Returns the normalized set of allowlisted admin email addresses.
     * Each entry is trimmed and lower-cased; blank entries are excluded.
     *
     * @return immutable set of canonical email strings (never {@code null})
     */
    public Set<String> getAllowedEmailSet() {
        if (allowedEmails == null || allowedEmails.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(allowedEmails.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(e -> !e.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    // ── Getter / Setter for Spring binding ───────────────────────────────────

    public String getAllowedEmails() {
        return allowedEmails;
    }

    public void setAllowedEmails(String allowedEmails) {
        this.allowedEmails = allowedEmails;
    }
}
