package com.app.auth.user.controller;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.filter.JwtAuthFilter;
import com.app.auth.auth.handler.OAuth2LoginSuccessHandler;
import com.app.auth.auth.service.JwtService;
import com.app.auth.auth.service.OAuthUserService;
import com.app.auth.cache.service.CacheService;
import com.app.auth.common.config.AdminProperties;
import com.app.auth.common.config.CorsConfig;
import com.app.auth.common.config.SecurityConfig;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, AdminProperties.class, CorsConfig.class, JwtAuthFilter.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private CacheService cacheService;
    @MockitoBean private UserMapper userMapper;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private com.app.auth.auth.service.TokenValidator tokenValidator;
    @MockitoBean private CookieFactory cookieFactory;
    @MockitoBean private OAuthUserService oAuthUserService;
    @MockitoBean private OAuth2LoginSuccessHandler successHandler;
    @MockitoBean private SessionRepository sessionRepository;

    @Test
    @DisplayName("GET /api/auth/me without auth returns 401 instead of redirecting to OAuth")
    void getCurrentUser_unauthenticated_returns401WithoutRedirect() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }
}
