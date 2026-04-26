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
 * <p>Modified for Hybrid Auth:
 * 1. Upsert User
 * 2. Generate Refresh Token (RT)
 * 3. Persist Session in Neo4j (linked to RT jti)
 * 4. Set RT in HttpOnly cookie
 * 5. Redirect to Dashboard (frontend will then call /refresh to get AT)
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
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String googleId = oAuth2User.getAttribute("sub");
        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");

        log.info(LogMessages.OAUTH2_LOGIN_SUCCESS, email);

        UserNode userNode = userService.upsertUser(googleId, email, name);

        // Generate RT ID
        String rtId = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(userNode.getId(), rtId);
        Instant rtExpiresAt = jwtService.getRefreshExpiryInstant();

        // Persist session tied to RT
        sessionService.createSession(userNode.getId(), rtId, rtExpiresAt);

        // Set Refresh Token Cookie
        ResponseCookie cookie = cookieFactory.buildRefreshTokenCookie(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("OAuth2 login successful for user {}, redirecting to dashboard", userNode.getId());
        response.sendRedirect(frontendUrl + "/dashboard");
    }
}
