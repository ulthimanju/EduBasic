package com.app.auth.auth.service;

import com.app.auth.cache.service.CacheService;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    @DisplayName("isTokenValid — cache hit valid → returns true")
    void isTokenValid_cacheHitValid_returnsTrue() {
        when(cacheService.getJwtValidity(JTI)).thenReturn(Optional.of(true));
        
        boolean valid = tokenValidator.isTokenValid(TOKEN, JTI);
        
        assertThat(valid).isTrue();
        verifyNoInteractions(sessionService, jwtService);
    }

    @Test
    @DisplayName("isTokenValid — cache hit invalid → returns false")
    void isTokenValid_cacheHitInvalid_returnsFalse() {
        when(cacheService.getJwtValidity(JTI)).thenReturn(Optional.of(false));
        
        boolean valid = tokenValidator.isTokenValid(TOKEN, JTI);
        
        assertThat(valid).isFalse();
        verifyNoInteractions(sessionService, jwtService);
    }

    @Test
    @DisplayName("isTokenValid — cache miss, session valid in DB → caches and returns true")
    void isTokenValid_cacheMissDbValid_cachesAndReturnsTrue() {
        when(cacheService.getJwtValidity(JTI)).thenReturn(Optional.empty());
        when(sessionService.isSessionValid(JTI)).thenReturn(true);
        
        boolean valid = tokenValidator.isTokenValid(TOKEN, JTI);
        
        assertThat(valid).isTrue();
        verify(cacheService).cacheJwtValidity(JTI, true);
    }

    @Test
    @DisplayName("isTokenValid — cache miss, session revoked in DB → invalidates with custom TTL and returns false")
    void isTokenValid_cacheMissDbRevoked_invalidatesAndReturnsFalse() {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(500);
        
        when(cacheService.getJwtValidity(JTI)).thenReturn(Optional.empty());
        when(sessionService.isSessionValid(JTI)).thenReturn(false);
        when(jwtService.extractExpiration(TOKEN)).thenReturn(expiry);
        
        boolean valid = tokenValidator.isTokenValid(TOKEN, JTI);
        
        assertThat(valid).isFalse();
        // verify it calls the overloaded cache method with ~500s TTL
        verify(cacheService).cacheJwtValidity(eq(JTI), eq(false), AdditionalMatchers.gt(490L));
    }

    @Test
    @DisplayName("invalidateToken — calculates remaining TTL correctly")
    void invalidateToken_calculatesTtl() {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(100);
        
        when(jwtService.extractExpiration(TOKEN)).thenReturn(expiry);
        
        tokenValidator.invalidateToken(TOKEN, JTI);
        
        // TTL should be around 100
        verify(cacheService).cacheJwtValidity(eq(JTI), eq(false), AdditionalMatchers.gt(95L));
    }
}
