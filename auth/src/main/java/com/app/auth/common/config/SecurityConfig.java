package com.app.auth.common.config;

import com.app.auth.auth.filter.JwtAuthFilter;
import com.app.auth.auth.handler.OAuth2LoginSuccessHandler;
import com.app.auth.auth.service.OAuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Spring Security configuration.
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

    public static RequestMatcher getPublicRoutes() {
        return new OrRequestMatcher(
                antMatcher("/oauth2/**"),
                antMatcher("/login/**"),
                antMatcher("/actuator/health"),
                antMatcher("/actuator/info"),
                antMatcher("/api/auth/.well-known/jwks.json"),
                antMatcher(HttpMethod.POST, "/api/auth/refresh"),
                antMatcher(HttpMethod.POST, "/api/auth/logout")
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable) // SameSite=Strict on RT cookie provides CSRF protection
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(getPublicRoutes()).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    antMatcher("/api/**")
                )
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(ui -> ui
                    .userService(oAuthUserService))
                .successHandler(successHandler)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
