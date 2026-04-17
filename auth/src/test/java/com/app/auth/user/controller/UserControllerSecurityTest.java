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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, AdminProperties.class, CorsConfig.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private CacheService cacheService;
    @MockBean private UserMapper userMapper;
    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private JwtService jwtService;
    @MockBean private CookieFactory cookieFactory;
    @MockBean private OAuthUserService oAuthUserService;
    @MockBean private OAuth2LoginSuccessHandler successHandler;
    @MockBean private SessionRepository sessionRepository;

    @Test
    @DisplayName("GET /api/auth/me without auth returns 401 instead of redirecting to OAuth")
    void getCurrentUser_unauthenticated_returns401WithoutRedirect() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }
}
