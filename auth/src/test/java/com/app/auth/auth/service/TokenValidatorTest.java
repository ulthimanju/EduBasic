package com.app.auth.auth.service;

import com.app.auth.cache.service.CacheService;
import com.app.auth.common.constants.CacheConstants;
import com.app.auth.session.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenValidatorTest {

    @Mock private JwtService jwtService;
    @Mock private CacheService cacheService;
    @Mock private SessionService sessionService;

    @InjectMocks
    private TokenValidator tokenValidator;

    private static final String TOKEN = "test.token.content";
    private static final String JTI = "test-jti";

    // ── Access Token Tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("isAccessTokenValid — cache hit valid → returns true (trusts short cache)")
    void isAccessTokenValid_cacheHitValid_returnsTrue() {
        when(cacheService.getJwtValidity(JTI)).thenReturn(Optional.of(true));

        boolean valid = tokenValidator.isAccessTokenValid(TOKEN, JTI);

        assertThat(valid).isTrue();
        verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("isAccessTokenValid — cache hit invalid → returns false")
    void isAccessTokenValid_cacheHitInvalid_returnsFalse() {
        when(cacheService.getJwtValidity(JTI)).thenReturn(Optional.of(false));

        boolean valid = tokenValidator.isAccessTokenValid(TOKEN, JTI);

        assertThat(valid).isFalse();
        verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("isAccessTokenValid — cache miss, DB valid → caches with short TTL and returns true")
    void isAccessTokenValid_cacheMissDbValid_cachesAndReturnsTrue() {
        when(cacheService.getJwtValidity(JTI)).thenReturn(Optional.empty());
        when(sessionService.isSessionValid(JTI)).thenReturn(true);

        boolean valid = tokenValidator.isAccessTokenValid(TOKEN, JTI);

        assertThat(valid).isTrue();
        verify(cacheService).cacheJwtValidity(JTI, true, CacheConstants.VALID_JWT_TTL_SECONDS);
    }

    // ── Refresh Token Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("isRefreshTokenValid — cache hit valid → IGNORES cache and checks DB (authoritative)")
    void isRefreshTokenValid_cacheHitValid_checksDb() {
        when(cacheService.getJwtValidity(JTI)).thenReturn(Optional.of(true));
        when(sessionService.isSessionValid(JTI)).thenReturn(true);

        boolean valid = tokenValidator.isRefreshTokenValid(TOKEN, JTI);

        assertThat(valid).isTrue();
        verify(sessionService).isSessionValid(JTI);
    }

    @Test
    @DisplayName("isRefreshTokenValid — cache hit invalid → returns false (trusts blacklist)")
    void isRefreshTokenValid_cacheHitInvalid_returnsFalse() {
        when(cacheService.getJwtValidity(JTI)).thenReturn(Optional.of(false));

        boolean valid = tokenValidator.isRefreshTokenValid(TOKEN, JTI);

        assertThat(valid).isFalse();
        verifyNoInteractions(sessionService);
    }

    @Test
    @DisplayName("isRefreshTokenValid — DB revoked → invalidates and returns false")
    void isRefreshTokenValid_dbRevoked_invalidatesAndReturnsFalse() {
        Instant expiry = Instant.now().plusSeconds(100);
        when(cacheService.getJwtValidity(JTI)).thenReturn(Optional.empty());
        when(sessionService.isSessionValid(JTI)).thenReturn(false);
        when(jwtService.extractExpiration(TOKEN)).thenReturn(expiry);

        boolean valid = tokenValidator.isRefreshTokenValid(TOKEN, JTI);

        assertThat(valid).isFalse();
        verify(cacheService).cacheJwtValidity(eq(JTI), eq(false), AdditionalMatchers.gt(95L));
    }

    // ── Invalidation ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("invalidateToken — calculates remaining TTL correctly")
    void invalidateToken_calculatesTtl() {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(100);

        when(jwtService.extractExpiration(TOKEN)).thenReturn(expiry);

        tokenValidator.invalidateToken(TOKEN, JTI);

        verify(cacheService).cacheJwtValidity(eq(JTI), eq(false), AdditionalMatchers.gt(95L));
    }
}
