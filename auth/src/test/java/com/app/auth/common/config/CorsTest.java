package com.app.auth.common.config;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.filter.JwtAuthFilter;
import com.app.auth.auth.service.JwtService;
import com.app.auth.cache.service.CacheService;
import com.app.auth.session.repository.SessionRepository;
import com.app.auth.user.mapper.UserMapper;
import com.app.auth.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CORS preflight tests confirming that {@code PATCH} and {@code DELETE} are
 * included in {@code Access-Control-Allow-Methods} after the CORS expansion.
 *
 * <p>Uses {@code OPTIONS} preflight requests from the configured frontend origin.</p>
 */
@WebMvcTest(controllers = com.app.auth.user.management.controller.UserManagementController.class)
@Import({SecurityConfig.class, AdminProperties.class, CorsConfig.class, JwtAuthFilter.class})
class CorsTest {

    @Autowired
    private MockMvc mockMvc;

    // Stub all dependencies to prevent context failure
    @MockitoBean private com.app.auth.user.management.service.UserManagementService userManagementService;
    @MockitoBean private com.app.auth.user.management.service.AdminAccessService    adminAccessService;
    @MockitoBean private JwtService     jwtService;
    @MockitoBean private com.app.auth.auth.service.TokenValidator tokenValidator;
    @MockitoBean private CacheService   cacheService;
    @MockitoBean private CookieFactory  cookieFactory;
    @MockitoBean private com.app.auth.auth.service.OAuthUserService        oAuthUserService;
    @MockitoBean private com.app.auth.auth.handler.OAuth2LoginSuccessHandler successHandler;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private UserService       userServiceBean;
    @MockitoBean private UserMapper        userMapperBean;

    // This must match ${app.frontend.url} — default in application.yml
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Test
    @DisplayName("CORS preflight for PATCH /api/user-management/users/{id} includes PATCH in allowed methods")
    void preflight_patchManagementEndpoint_includesPatch() throws Exception {
        mockMvc.perform(options("/api/user-management/users/uid-1")
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        org.hamcrest.Matchers.containsString("PATCH")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS preflight for DELETE /api/user-management/users/{id} includes DELETE in allowed methods")
    void preflight_deleteManagementEndpoint_includesDelete() throws Exception {
        mockMvc.perform(options("/api/user-management/users/uid-1")
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        org.hamcrest.Matchers.containsString("DELETE")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS preflight from unknown origin is rejected")
    void preflight_unknownOrigin_notAllowed() throws Exception {
        mockMvc.perform(options("/api/user-management/users/uid-1")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
