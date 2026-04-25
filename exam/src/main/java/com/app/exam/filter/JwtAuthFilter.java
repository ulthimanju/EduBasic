package com.app.exam.filter;

import com.app.exam.service.JwtService;
import com.app.exam.service.TokenValidationService;
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
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenValidationService tokenValidationService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        Optional<String> jwtOpt = extractJwtFromHeader(request);

        if (jwtOpt.isPresent()) {
            String jwt = jwtOpt.get();
            if (jwtService.validateToken(jwt)) {
                String jwtId = jwtService.extractJwtId(jwt);
                
                if (tokenValidationService.isRevoked(jwtId)) {
                    log.debug("Rejecting revoked token: {}", jwtId);
                    chain.doFilter(request, response);
                    return;
                }

                String userId = jwtService.extractUserId(jwt);
                List<String> roles = jwtService.extractRoles(jwt);
                
                List<SimpleGrantedAuthority> authorities = roles != null ?
                        roles.stream().map(SimpleGrantedAuthority::new).toList() : List.of();

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(UUID.fromString(userId), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

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
