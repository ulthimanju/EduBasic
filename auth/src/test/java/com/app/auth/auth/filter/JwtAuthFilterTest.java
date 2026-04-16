package com.app.auth.auth.filter;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.service.JwtService;
import com.app.auth.cache.service.CacheService;
import com.app.auth.common.config.SecurityConfig;
import com.app.auth.session.repository.SessionRepository;
import com.app.auth.user.dto.UserResponseDTO;
import com.app.auth.user.mapper.UserMapper;
import com.app.auth.user.node.UserNode;
import com.app.auth.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock private CacheService      cacheService;
    @Mock private SessionRepository sessionRepository;
    @Mock private UserService       userService;
    @Mock private UserMapper        userMapper;
    @Mock private CookieFactory     cookieFactory;
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

    private ResponseCookie buildClearCookie() {
        return ResponseCookie.from("auth_token", "").maxAge(0).path("/").build();
    }

    // ── Public route tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("Public route (POST /api/auth/logout) with a cookie skips filter entirely")
    void publicRoute_logoutWithCookie_passesThrough() throws Exception {
        // servletPath must be set so AntPathRequestMatcher can derive the path
        MockHttpServletRequest  request  = buildRequest("POST", "/api/auth/logout", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Verify the matcher works correctly with our MockHttpServletRequest setup
        assertThat(SecurityConfig.PUBLIC_ROUTES.matches(request)).isTrue();

        filter.doFilterInternal(request, response, filterChain);

        // Chain must proceed without any token inspection
        verify(filterChain).doFilter(request, response);
        verify(cookieFactory, never()).extractJwtFromRequest(any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Public route (GET /oauth2/**) skips filter regardless of cookie")
    void publicRoute_oauthRoute_passesThrough() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/oauth2/authorization/google", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(SecurityConfig.PUBLIC_ROUTES.matches(request)).isTrue();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(cookieFactory, never()).extractJwtFromRequest(any());
    }

    @Test
    @DisplayName("Public route (/actuator/health) skips filter")
    void publicRoute_actuatorHealth_passesThrough() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/actuator/health", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(SecurityConfig.PUBLIC_ROUTES.matches(request)).isTrue();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(cookieFactory, never()).extractJwtFromRequest(any());
    }

    // ── Protected route — malformed token ────────────────────────────────────

    @Test
    @DisplayName("Protected route with malformed JWT returns 401 and clears cookie")
    void protectedRoute_malformedJwt_returns401AndClearsCookie() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/api/user-management/users", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(cookieFactory.extractJwtFromRequest(request)).thenReturn(Optional.of(VALID_JWT));
        when(jwtService.extractJwtId(VALID_JWT)).thenThrow(new RuntimeException("Malformed token"));
        when(cookieFactory.clearAuthCookie()).thenReturn(buildClearCookie());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNotNull();
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Protected route with JWT cached as invalid returns 401 and clears cookie")
    void protectedRoute_cachedInvalidJwt_returns401AndClearsCookie() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/api/user-management/users", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(cookieFactory.extractJwtFromRequest(request)).thenReturn(Optional.of(VALID_JWT));
        when(jwtService.extractJwtId(VALID_JWT)).thenReturn(JWT_ID);
        when(cacheService.getJwtValidity(JWT_ID)).thenReturn(Optional.of(false));
        when(cookieFactory.clearAuthCookie()).thenReturn(buildClearCookie());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNotNull();
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ── Protected route — valid JWT, missing user ─────────────────────────────

    @Test
    @DisplayName("Protected route with valid JWT but deleted user returns 401, marks JWT invalid, clears cookie")
    void protectedRoute_validJwtMissingUser_returns401MarksInvalidClearsCookie() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/api/user-management/users", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(cookieFactory.extractJwtFromRequest(request)).thenReturn(Optional.of(VALID_JWT));
        when(jwtService.extractJwtId(VALID_JWT)).thenReturn(JWT_ID);
        when(cacheService.getJwtValidity(JWT_ID)).thenReturn(Optional.of(true)); // cached valid
        when(jwtService.extractUserId(VALID_JWT)).thenReturn(USER_ID);
        when(cacheService.getCachedUserProfile(USER_ID)).thenReturn(Optional.empty());
        when(userService.findById(USER_ID)).thenReturn(Optional.empty()); // user gone!
        when(cookieFactory.clearAuthCookie()).thenReturn(buildClearCookie());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNotNull();
        verify(cacheService).evictUserCache(USER_ID);
        verify(cacheService).cacheJwtValidity(JWT_ID, false);
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── Protected route — fully valid path ───────────────────────────────────

    @Test
    @DisplayName("Protected route with valid JWT and present user populates SecurityContext")
    void protectedRoute_validJwt_setsSecurityContext() throws Exception {
        MockHttpServletRequest  request  = buildRequest("GET", "/api/user/me", VALID_JWT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserNode userNode = UserNode.builder()
                .id(USER_ID).email("user@example.com").name("Test User").build();
        UserResponseDTO dto = new UserResponseDTO(USER_ID, "user@example.com", "Test User");

        when(cookieFactory.extractJwtFromRequest(request)).thenReturn(Optional.of(VALID_JWT));
        when(jwtService.extractJwtId(VALID_JWT)).thenReturn(JWT_ID);
        when(cacheService.getJwtValidity(JWT_ID)).thenReturn(Optional.of(true)); // cached valid
        when(jwtService.extractUserId(VALID_JWT)).thenReturn(USER_ID);
        when(cacheService.getCachedUserProfile(USER_ID)).thenReturn(Optional.empty());
        when(userService.findById(USER_ID)).thenReturn(Optional.of(userNode));
        when(userMapper.toResponseDTO(userNode)).thenReturn(dto);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(USER_ID);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a {@link MockHttpServletRequest} with both {@code requestURI} and
     * {@code servletPath} set. Both are required for {@link org.springframework.security
     * .web.util.matcher.AntPathRequestMatcher} to resolve the path correctly.
     */
    private MockHttpServletRequest buildRequest(String method, String path, String cookieValue) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        // AntPathRequestMatcher uses servletPath (falls back to requestURI when blank)
        request.setServletPath(path);
        if (cookieValue != null) {
            request.setCookies(new Cookie("auth_token", cookieValue));
        }
        return request;
    }
}
