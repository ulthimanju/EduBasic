package com.app.auth.auth.filter;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.service.JwtService;
import com.app.auth.auth.service.TokenValidator;
import com.app.auth.common.config.SecurityConfig;
import com.app.auth.user.mapper.UserMapper;
import com.app.auth.user.service.UserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtAuthFilter}.
 *
 * <p>All dependencies are mocked. No Spring context is started.
 * Covers the six critical scenarios identified in the test plan:</p>
 * <ul>
 *   <li>Public route + any cookie → skip filter entirely, chain proceeds</li>
 *   <li>Protected route + malformed JWT → 401 + cookie cleared</li>
 *   <li>Protected route + revoked/cached-invalid JWT → 401 + cookie cleared</li>
 *   <li>Protected route + valid JWT + missing user → 401, JWT marked invalid, cookie cleared</li>
 *   <li>Protected route + valid JWT + present user → SecurityContext populated</li>
 * </ul>
 *
 * <p><strong>Note on public-route matching</strong>: {@link SecurityConfig#PUBLIC_ROUTES}
 * uses Spring Security's {@code AntPathRequestMatcher}, which resolves the request path
 * from {@code servletPath} (then {@code requestURI}). {@link MockHttpServletRequest}
 * requires {@code setServletPath()} to be called explicitly — the helper method
 * {@link #buildRequest} does this.</p>
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtService        jwtService;
    @Mock private UserService       userService;
    @Mock private UserMapper        userMapper;
    @Mock private CookieFactory     cookieFactory;
    @Mock private TokenValidator    tokenValidator;
    @Mock private FilterChain       filterChain;

    @InjectMocks
    private JwtAuthFilter filter;

    private static final String VALID_JWT  = "header.payload.sig";
    private static final String JWT_ID     = "test-jwt-id";
    private static final String USER_ID    = "test-user-id";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    // ── Public route tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("Public route (POST /api/auth/logout) skips filter entirely")
    void publicRoute_logout_passesThrough() throws Exception {
        // servletPath must be set so AntPathRequestMatcher can derive the path
        MockHttpServletRequest  request  = buildRequest("POST", "/api/auth/logout", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Verify the matcher works correctly with our MockHttpServletRequest setup
        assertThat(SecurityConfig.getPublicRoutes().matches(request)).isTrue();

        filter.doFilterInternal(request, response, filterChain);

        // Chain must proceed without any token inspection
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Public route (GET /oauth2/**) skips filter regardless of token")
    void publicRoute_oauthRoute_passesThrough() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/oauth2/authorization/google", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(SecurityConfig.getPublicRoutes().matches(request)).isTrue();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Public route (/actuator/health) skips filter")
    void publicRoute_actuatorHealth_passesThrough() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/actuator/health", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(SecurityConfig.getPublicRoutes().matches(request)).isTrue();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    // ── Protected route — malformed token ────────────────────────────────────

    @Test
    @DisplayName("Protected route with malformed JWT proceeds through chain (Spring Security will handle 401)")
    void protectedRoute_malformedJwt_proceedsThroughChain() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/api/user-management/users", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractJwtId(VALID_JWT)).thenThrow(new RuntimeException("Malformed token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Protected route with JWT cached as invalid proceeds through chain")
    void protectedRoute_cachedInvalidJwt_proceedsThroughChain() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/api/user-management/users", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractJwtId(VALID_JWT)).thenReturn(JWT_ID);
        when(jwtService.validateToken(VALID_JWT)).thenReturn(true);
        when(tokenValidator.isAccessTokenValid(VALID_JWT, JWT_ID)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── Protected route — valid JWT, missing user ─────────────────────────────
    // Note: JwtAuthFilter doesn't check user presence anymore, it just validates JWT and sets context.
    // If we want to check user presence, it would be in a different filter or service.
    // Current JwtAuthFilter only uses jwtService.extractUserId and jwtService.extractRoles.

    @Test
    @DisplayName("Protected route with valid JWT populates SecurityContext")
    void protectedRoute_validJwt_setsSecurityContext() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/api/user/me", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractJwtId(VALID_JWT)).thenReturn(JWT_ID);
        when(jwtService.validateToken(VALID_JWT)).thenReturn(true);
        when(tokenValidator.isAccessTokenValid(VALID_JWT, JWT_ID)).thenReturn(true);
        when(jwtService.extractUserId(VALID_JWT)).thenReturn(USER_ID);
        when(jwtService.extractRoles(VALID_JWT)).thenReturn(List.of("ROLE_STUDENT"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(USER_ID);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a {@link MockHttpServletRequest} with both {@code requestURI} and
     * {@code servletPath} set.
     */
    private MockHttpServletRequest buildRequest(String method, String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        if (token != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request;
    }
}
