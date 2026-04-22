package com.app.auth.user.controller;

import com.app.auth.LogMessages;
import com.app.auth.cache.service.CacheService;
import com.app.auth.common.exception.UserNotFoundException;
import com.app.auth.common.response.ApiResponse;
import com.app.auth.user.dto.UserResponseDTO;
import com.app.auth.user.mapper.UserMapper;
import com.app.auth.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile endpoints.
 *
 * <p>Only one endpoint: {@code GET /api/auth/me} — returns the authenticated
 * user's profile, using a cache-first lookup.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService  userService;
    private final CacheService cacheService;
    private final UserMapper   userMapper;

    /**
     * Returns the profile of the currently authenticated user.
     *
     * <p>The userId is extracted from the SecurityContext (populated by JwtAuthFilter).
     * Lookup is cache-first: Redis hit skips Neo4j entirely.</p>
     *
     * @param userId injected from SecurityContext principal by Spring Security
     * @return {@code 200 OK} with user DTO, or {@code 404} if the user was deleted
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser(
            @AuthenticationPrincipal String userId) {

        log.debug(LogMessages.GET_ME_USER_ID, userId);

        // Cache-first lookup
        UserResponseDTO user = cacheService.getCachedUserProfile(userId)
                .orElseGet(() -> {
                    UserResponseDTO fetched = userService.findById(userId)
                            .map(userMapper::toResponseDTO)
                            .orElseThrow(() -> new UserNotFoundException(
                                    "User not found: " + userId));
                    cacheService.cacheUserProfile(userId, fetched);
                    return fetched;
                });

        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
