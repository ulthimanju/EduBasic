package com.app.auth.auth.filter;

import com.app.auth.auth.service.JwtService;
import com.app.auth.auth.service.TokenValidator;
import com.app.auth.cache.service.CacheService;
import com.app.auth.common.config.SecurityConfig;
import com.app.auth.session.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Validates the Authorization: Bearer token and populates the SecurityContext.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService        jwtService;
    private final CacheService      cacheService;
    private final SessionRepository sessionRepository;
    private final TokenValidator    tokenValidator;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest  request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain         chain)
            throws ServletException, IOException {

        if (SecurityConfig.getPublicRoutes().matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<String> jwtOpt = extractJwtFromHeader(request);
        if (jwtOpt.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = jwtOpt.get();
        String jwtId;
        try {
            jwtId = jwtService.extractJwtId(jwt);
        } catch (Exception e) {
            chain.doFilter(request, response);
            return;
        }

        if (!jwtService.validateToken(jwt)) {
            tokenValidator.invalidateToken(jwt, jwtId);
            chain.doFilter(request, response);
            return;
        }

        // Check cache and Neo4j
        if (!tokenValidator.isTokenValid(jwt, jwtId)) {
            chain.doFilter(request, response);
            return;
        }

        // Populate SecurityContext with roles
        String userId = jwtService.extractUserId(jwt);
        List<String> roles = jwtService.extractRoles(jwt);
        
        List<SimpleGrantedAuthority> authorities = roles != null ?
                roles.stream().map(SimpleGrantedAuthority::new).toList() : List.of();

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        chain.doFilter(request, response);
    }

    private Optional<String> extractJwtFromHeader(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }
}
