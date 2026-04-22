package com.app.auth.auth.handler;

import com.app.auth.LogMessages;
import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.service.JwtService;
import com.app.auth.cache.service.CacheService;
import com.app.auth.session.service.SessionService;
import com.app.auth.user.node.UserNode;
import com.app.auth.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Called by Spring Security after a successful Google OAuth callback.
 *
 * <p>Algorithm (§9.4 of design doc):
 * <ol>
 *   <li>Extract OAuth2User from Authentication</li>
 *   <li>Read googleId, email, name from attributes</li>
 *   <li>Upsert user in Neo4j via UserService</li>
 *   <li>Generate jwtId = UUID</li>
 *   <li>Generate JWT string via JwtService</li>
 *   <li>Create Session node + HAS_SESSION edge in Neo4j</li>
 *   <li>Cache JWT as "valid" in Redis</li>
 *   <li>Set HttpOnly auth cookie on response</li>
 *   <li>Redirect to frontend /dashboard</li>
 * </ol>
 *
 * <p><strong>IMPORTANT:</strong> If any Neo4j write fails (steps 3 or 6),
 * the exception propagates and the cookie is NOT set. The user sees an error
 * rather than receiving a token for a session that was never persisted.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService    userService;
    private final JwtService     jwtService;
    private final SessionService sessionService;
    private final CacheService   cacheService;
    private final CookieFactory  cookieFactory;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest  request,
                                        HttpServletResponse response,
                                        Authentication      authentication) throws IOException {
        // 1. Extract Google user principal
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String googleId = oAuth2User.getAttribute("sub");
        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");

        log.info(LogMessages.OAUTH2_LOGIN_SUCCESS, email);

        // 2. Upsert user — creates on first login, updates lastLogin on subsequent logins
        UserNode userNode = userService.upsertUser(googleId, email, name);

        // 3. Generate a unique token ID (jti claim = Neo4j sessionId)
        String jwtId = UUID.randomUUID().toString();

        // 4. Sign the application JWT
        String jwtString = jwtService.generateToken(userNode.getId(), email, jwtId);

        // 5. Compute expiry Instant for Neo4j session node
        Instant expiresAt = jwtService.getExpiryInstant();

        // 6. Persist session in Neo4j (may throw — cookie NOT set if this fails)
        sessionService.createSession(userNode.getId(), jwtId, expiresAt);

        // 7. Pre-warm Redis cache with valid status
        cacheService.cacheJwtValidity(jwtId, true);

        // 8. Set HttpOnly auth cookie on response
        ResponseCookie cookie = cookieFactory.buildAuthCookie(jwtString);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 9. Redirect to dashboard
        log.info(LogMessages.AUTH_COOKIE_SET_REDIRECTING, userNode.getId());
        response.sendRedirect(frontendUrl + "/dashboard");
    }
}
