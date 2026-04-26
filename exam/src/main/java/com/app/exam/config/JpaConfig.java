package com.app.exam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@org.springframework.data.jpa.repository.config.EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.empty();
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof UUID) {
                return Optional.of((UUID) principal);
            }
            try {
                return Optional.of(UUID.fromString(principal.toString()));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }
}
