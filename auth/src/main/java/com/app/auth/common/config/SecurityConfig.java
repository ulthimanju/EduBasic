package com.app.auth.common.config;

import com.app.auth.auth.filter.JwtAuthFilter;
import com.app.auth.auth.handler.OAuth2LoginSuccessHandler;
import com.app.auth.auth.service.OAuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Spring Security configuration.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>Session policy: STATELESS — no server-side HTTP session, tokens only</li>
 *   <li>Form login and HTTP Basic explicitly disabled</li>
 *   <li>CSRF disabled — stateless JWT in HttpOnly cookie, SameSite=Strict covers CSRF</li>
 *   <li>OAuth2 login wired with custom user service and success handler</li>
 *   <li>JwtAuthFilter runs before UsernamePasswordAuthenticationFilter</li>
 *   <li>{@code @EnableMethodSecurity} activates {@code @PreAuthorize} on management routes</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AdminProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter             jwtAuthFilter;
    private final OAuthUserService          oAuthUserService;
    private final OAuth2LoginSuccessHandler successHandler;

    /**
     * The canonical permit-all matcher shared between this config and {@link JwtAuthFilter}.
     *
     * <p>Both places must agree: any route listed here is never blocked by the JWT filter
     * and is also reachable without authentication in the filter chain. Adding a route in
     * only one place will cause an inconsistency.</p>
     */
    public static final RequestMatcher PUBLIC_ROUTES = new OrRequestMatcher(
            new AntPathRequestMatcher("/oauth2/**"),
            new AntPathRequestMatcher("/login/**"),
            new AntPathRequestMatcher("/actuator/health"),
            new AntPathRequestMatcher("/actuator/info"),
            new AntPathRequestMatcher("/api/auth/logout", "POST")
    );

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── Session ──────────────────────────────────────────────────────
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Disable unused auth mechanisms ───────────────────────────────
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            // ── CSRF: off (stateless + SameSite cookie) ───────────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS: delegate to CorsConfig bean ─────────────────────────────
            .cors(cors -> {})

            // ── Authorization rules ───────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ROUTES).permitAll()
                .anyRequest().authenticated()
            )

            // ── API auth failures should return 401, not redirect to Google ──
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")
                )
            )

            // ── OAuth2 login flow ─────────────────────────────────────────────
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(ui -> ui
                    .userService(oAuthUserService))
                .successHandler(successHandler)
            )

            // ── Add JWT filter before the default auth filter ─────────────────
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
