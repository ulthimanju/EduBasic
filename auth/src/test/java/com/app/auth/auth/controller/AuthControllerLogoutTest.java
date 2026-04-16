package com.app.auth.auth.controller;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.service.JwtService;
import com.app.auth.cache.service.CacheService;
import com.app.auth.session.repository.SessionRepository;
import com.app.auth.session.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link AuthController#logout} endpoint.
 *
 * <p>Verifies best-effort logout behaviour:
 * <ul>
 *   <li>No cookie  → 200 + cookie cleared</li>
 *   <li>Malformed cookie → 200 + cookie cleared (no exception propagates)</li>
 *   <li>Valid cookie → 200 + session revoked + caches evicted + cookie cleared</li>
 * </ul>
 *
 * <p>{@code @WebMvcTest} spins up the web layer. Security auto-configuration is
 * excluded to keep tests focused on controller logic. All beans required by
 * {@code JwtAuthFilter} (which is a {@code @Component} in the scan) are mocked.</p>
 */
@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
        })
class AuthControllerLogoutTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService      jwtService;
    @MockBean private SessionService  sessionService;
    @MockBean private CacheService    cacheService;
    @MockBean private CookieFactory   cookieFactory;

    // JwtAuthFilter is a @Component — its dependencies must be mocked to avoid context failure
    @MockBean private SessionRepository sessionRepository;
    @MockBean private com.app.auth.user.service.UserService userService;
    @MockBean private com.app.auth.user.mapper.UserMapper   userMapper;

    private static final String VALID_JWT = "header.payload.sig";
    private static final String JWT_ID    = "test-jwt-id";
    private static final String USER_ID   = "test-user-id";

    private ResponseCookie buildClearCookie() {
        return ResponseCookie.from("auth_token", "").maxAge(0).path("/").build();
    }

    @Test
    @DisplayName("logout — no cookie present → 200 + cookie cleared")
    void logout_noCookie_returns200AndClearsCookie() throws Exception {
        when(cookieFactory.clearAuthCookie()).thenReturn(buildClearCookie());
        when(cookieFactory.extractJwtFromRequest(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("auth_token=")));

        // No Neo4j/Redis work should happen
        verifyNoInteractions(sessionService, jwtService, cacheService);
    }

    @Test
    @DisplayName("logout — malformed cookie → 200 + cookie cleared (no exception)")
    void logout_malformedCookie_returns200AndClearsCookie() throws Exception {
        when(cookieFactory.clearAuthCookie()).thenReturn(buildClearCookie());
        when(cookieFactory.extractJwtFromRequest(any())).thenReturn(Optional.of("bad.token"));
        when(jwtService.extractJwtId("bad.token")).thenThrow(new RuntimeException("Malformed JWT"));

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("auth_token", "bad.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("auth_token=")));

        // Session service must not be called
        verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("logout — valid cookie → 200 + session revoked + caches evicted + cookie cleared")
    void logout_validCookie_revokesSessionAndClearsCookie() throws Exception {
        when(cookieFactory.clearAuthCookie()).thenReturn(buildClearCookie());
        when(cookieFactory.extractJwtFromRequest(any())).thenReturn(Optional.of(VALID_JWT));
        when(jwtService.extractJwtId(VALID_JWT)).thenReturn(JWT_ID);
        when(jwtService.extractUserId(VALID_JWT)).thenReturn(USER_ID);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("auth_token", VALID_JWT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("auth_token=")));

        verify(sessionService).revokeSession(JWT_ID);
        verify(cacheService).evictJwtCache(JWT_ID);
        verify(cacheService).evictUserCache(USER_ID);
    }
}
