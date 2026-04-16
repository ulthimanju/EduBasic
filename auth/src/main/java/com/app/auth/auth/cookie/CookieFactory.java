package com.app.auth.auth.cookie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

/**
 * Factory for the {@code auth_token} HttpOnly cookie.
 *
 * <p>Two operations only:
 * <ul>
 *   <li>{@link #buildAuthCookie} — creates a cookie carrying the JWT</li>
 *   <li>{@link #clearAuthCookie} — creates a Max-Age=0 cookie to expire it</li>
 * </ul>
 *
 * <p>The {@code Secure} flag is controlled by {@code app.cookie.secure} — set
 * {@code false} for local HTTP dev, {@code true} for production HTTPS.</p>
 */
@Component
@Slf4j
public class CookieFactory {

    public static final String COOKIE_NAME = "auth_token";

    @Value("${app.jwt.expiration-seconds}")
    private long expirationSeconds;

    @Value("${app.cookie.secure:false}")
    private boolean secure;

    /**
     * Build an HttpOnly auth cookie carrying the signed JWT.
     *
     * @param jwtString compact JWT string (never logged)
     * @return {@link ResponseCookie} ready for Set-Cookie header
     */
    public ResponseCookie buildAuthCookie(String jwtString) {
        return ResponseCookie.from(COOKIE_NAME, jwtString)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(expirationSeconds)
                .build();
    }

    /**
     * Build a Max-Age=0 cookie that instructs the browser to delete {@code auth_token}.
     *
     * @return {@link ResponseCookie} with blank value and zero max-age
     */
    public ResponseCookie clearAuthCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    /**
     * Extract the raw JWT string from the request cookies.
     *
     * @param request incoming HTTP request
     * @return Optional containing the JWT value, or empty if no cookie found
     */
    public Optional<String> extractJwtFromRequest(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> !v.isBlank())
                .findFirst();
    }
}
