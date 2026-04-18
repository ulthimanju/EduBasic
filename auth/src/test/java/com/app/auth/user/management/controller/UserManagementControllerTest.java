package com.app.auth.user.management.controller;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.service.JwtService;
import com.app.auth.cache.service.CacheService;
import com.app.auth.session.repository.SessionRepository;
import com.app.auth.user.dto.UserResponseDTO;
import com.app.auth.user.management.service.AdminAccessService;
import com.app.auth.user.management.service.UserManagementService;
import com.app.auth.user.mapper.UserMapper;
import com.app.auth.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security slice tests for {@link UserManagementController}.
 *
 * <p>Tests that {@code @PreAuthorize("@adminAccessService.isAdmin(authentication)")}
 * correctly gates each endpoint:
 * <ul>
 *   <li>Unauthenticated → 401</li>
 *   <li>Authenticated but non-admin → 403</li>
 *   <li>Authenticated admin → 200</li>
 * </ul>
 *
 * <p><em>Context strategy</em>: {@code @WebMvcTest} keeps full security auto-config
 * (so {@code HttpSecurity} bean is available). {@link SliceSecurityConfig} contributes
 * a stateless {@code SecurityFilterChain} and {@code @EnableMethodSecurity}, while the
 * OAuth2 client auto-config is excluded to avoid needing real Google credentials.
 * The chain uses a custom 401 entry-point so unauthenticated requests return JSON, not
 * a 302 redirect.</p>
 */
@WebMvcTest(
        controllers = UserManagementController.class,
        excludeAutoConfiguration =
                org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
)
@Import(UserManagementControllerTest.SliceSecurityConfig.class)
class UserManagementControllerTest {

    /**
     * Provides a custom, stateless {@code SecurityFilterChain} and activates
     * {@code @EnableMethodSecurity}.
     *
     * <p>Does <em>not</em> exclude {@code SecurityAutoConfiguration} — keeping it active
     * ensures {@code HttpSecurity} and {@code WebSecurityConfiguration} are available.
     * This {@code SecurityFilterChain} takes precedence over Spring Boot's default one
     * because it will be the only one registered for this context.</p>
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class SliceSecurityConfig {

        @Bean
        SecurityFilterChain sliceFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}");
                        })
                );
            return http.build();
        }
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserManagementService                    userManagementService;
    @MockitoBean(name = "adminAccessService") private AdminAccessService adminAccessService;

    // JwtAuthFilter (@Component) is in component scan — stub its constructor deps
    @MockitoBean private JwtService        jwtService;
    @MockitoBean private CacheService      cacheService;
    @MockitoBean private CookieFactory     cookieFactory;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private UserService       userService;
    @MockitoBean private UserMapper        userMapper;
    // OAuthUserService and successHandler needed by OAuth2 login config stubs
    @MockitoBean private com.app.auth.auth.service.OAuthUserService        oAuthUserService;
    @MockitoBean private com.app.auth.auth.handler.OAuth2LoginSuccessHandler successHandler;

    private static final UserResponseDTO SAMPLE_USER =
            new UserResponseDTO("uid-1", "admin@example.com", "Admin User");

    // ── Unauthenticated (401) ─────────────────────────────────────────────────

    @Test
    @DisplayName("Unauthenticated request → 401")
    void getAllUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/user-management/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── Non-admin (403) ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /users — non-admin user → 403")
    @WithMockUser(username = "non-admin-user-id")
    void getAllUsers_nonAdminUser_returns403() throws Exception {
        when(adminAccessService.isAdmin(any())).thenReturn(false);

        mockMvc.perform(get("/api/user-management/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users/{id} — non-admin user → 403")
    @WithMockUser(username = "non-admin-user-id")
    void getUserById_nonAdminUser_returns403() throws Exception {
        when(adminAccessService.isAdmin(any())).thenReturn(false);

        mockMvc.perform(get("/api/user-management/users/uid-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /users/{id} — non-admin user → 403")
    @WithMockUser(username = "non-admin-user-id")
    void deleteUser_nonAdminUser_returns403() throws Exception {
        when(adminAccessService.isAdmin(any())).thenReturn(false);

        mockMvc.perform(delete("/api/user-management/users/uid-1"))
                .andExpect(status().isForbidden());
    }

    // ── Admin (200) ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /users — admin user → 200 with user list")
    @WithMockUser(username = "admin-user-id")
    void getAllUsers_adminUser_returns200() throws Exception {
        when(adminAccessService.isAdmin(any())).thenReturn(true);
        when(userManagementService.getAllUsers()).thenReturn(List.of(SAMPLE_USER));

        mockMvc.perform(get("/api/user-management/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("uid-1"));
    }

    @Test
    @DisplayName("PATCH /users/{id} — admin user → 200 with updated user")
    @WithMockUser(username = "admin-user-id")
    void updateUser_adminUser_returns200() throws Exception {
        when(adminAccessService.isAdmin(any())).thenReturn(true);
        when(userManagementService.updateUserName("uid-1", "New Name"))
                .thenReturn(new UserResponseDTO("uid-1", "admin@example.com", "New Name"));

        mockMvc.perform(patch("/api/user-management/users/uid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"));
    }

    @Test
    @DisplayName("DELETE /users/{id} — admin user → 200")
    @WithMockUser(username = "admin-user-id")
    void deleteUser_adminUser_returns200() throws Exception {
        when(adminAccessService.isAdmin(any())).thenReturn(true);

        mockMvc.perform(delete("/api/user-management/users/uid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted"));
    }
}
