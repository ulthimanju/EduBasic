package com.nexora.courseservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.setSessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Management endpoints (Instructor/Admin)
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses", "/api/v1/courses/*/modules", "/api/v1/modules/*/lessons", "/api/v1/courses/*/exams").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/courses/**", "/api/v1/modules/**", "/api/v1/lessons/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/**", "/api/v1/modules/**", "/api/v1/lessons/**").hasAnyAuthority("INSTRUCTOR", "ADMIN")
                        
                        // Student endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/enroll").hasAuthority("STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/lessons/*/progress").hasAuthority("STUDENT")
                        .requestMatchers("/api/v1/me/**").hasAuthority("STUDENT")
                        
                        // Public/Authenticated Shared
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**", "/api/v1/courses/*/outline").authenticated()
                        .requestMatchers("/actuator/health").permitAll()
                        
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
