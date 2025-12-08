package com.edubas.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edubas.backend.dto.AuthResponse;
import com.edubas.backend.dto.LoginRequest;
import com.edubas.backend.dto.LogoutRequest;
import com.edubas.backend.dto.RegisterRequest;
import com.edubas.backend.entity.User;
import com.edubas.backend.service.AuditLogService;
import com.edubas.backend.service.UserService;
import com.edubas.backend.util.JwtTokenProvider;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
        private final UserService userService;
        private final AuditLogService auditLogService;
        private final JwtTokenProvider jwtTokenProvider;

        public AuthController(UserService userService, AuditLogService auditLogService,
                        JwtTokenProvider jwtTokenProvider) {
                this.userService = userService;
                this.auditLogService = auditLogService;
                this.jwtTokenProvider = jwtTokenProvider;
        }

        @GetMapping("/me")
        public ResponseEntity<AuthResponse> me(Authentication authentication) {
                if (authentication == null || authentication.getName() == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(new AuthResponse("error", "Unauthorized", null, null, null, null, null,
                                                        null, null, null));
                }

                try {
                        String username = authentication.getName();
                        User user = userService.getUserByUsername(username);

                        AuthResponse response = new AuthResponse(
                                        "success",
                                        "User fetched",
                                        null,
                                        user.getId(),
                                        user.getUsername(),
                                        user.getEmail(),
                                        user.getRole(),
                                        user.getAvatar(),
                                        user.getProfileVisibility(),
                                        user.getEmailNotifications());

                        return ResponseEntity.ok(response);
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new AuthResponse("error", e.getMessage(), null, null, null, null, null,
                                                        null, null, null));
                } catch (Exception e) {
                        logger.error("Error fetching current user: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new AuthResponse("error", "Failed to fetch user: " + e.getMessage(),
                                                        null, null, null, null, null, null, null, null));
                }
        }

        @PostMapping("/register")
        public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
                logger.info("Registration request received for username: {}", request.getUsername());

                try {
                        // Save user to Neo4j database
                        User user = userService.registerUser(
                                        request.getUsername(),
                                        request.getEmail(),
                                        request.getPassword(),
                                        request.getRole());

                        // Generate JWT token
                        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getEmail(),
                                        user.getRole());

                        AuthResponse response = new AuthResponse(
                                        "success",
                                        "User registered successfully",
                                        token,
                                        user.getId(),
                                        user.getUsername(),
                                        user.getEmail(),
                                        user.getRole(),
                                        user.getAvatar(),
                                        user.getProfileVisibility(),
                                        user.getEmailNotifications());

                        logger.info("User registered and token generated for: {}", user.getUsername());
                        return ResponseEntity.ok(response);

                } catch (IllegalArgumentException e) {
                        auditLogService.logFailedLogin(
                                        request.getUsername() != null ? request.getUsername() : request.getEmail(),
                                        e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                                        new AuthResponse("error", e.getMessage(), null, null, null, null, null, null,
                                                        null, null));
                } catch (Exception e) {
                        logger.error("Error during registration: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        new AuthResponse("error", "Registration failed: " + e.getMessage(), null, null,
                                                        null, null, null,
                                                        null, null, null));
                }
        }

        @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
                logger.info("Login request received for: {}", request.getUsernameOrEmail());

                try {
                        // Authenticate user
                        User user = userService.loginUser(
                                        request.getUsernameOrEmail(),
                                        request.getPassword());

                        // Generate JWT token
                        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getEmail(),
                                        user.getRole());

                        AuthResponse response = new AuthResponse(
                                        "success",
                                        "Login successful",
                                        token,
                                        user.getId(),
                                        user.getUsername(),
                                        user.getEmail(),
                                        user.getRole(),
                                        user.getAvatar(),
                                        user.getProfileVisibility(),
                                        user.getEmailNotifications());

                        logger.info("User logged in successfully and token generated for: {}", user.getUsername());
                        return ResponseEntity.ok(response);

                } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                        new AuthResponse("error", e.getMessage(), null, null, null, null, null, null,
                                                        null, null));
                } catch (Exception e) {
                        logger.error("Error during login: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        new AuthResponse("error", "Login failed: " + e.getMessage(), null, null, null,
                                                        null, null, null,
                                                        null, null));
                }
        }

        @PostMapping("/logout")
        public ResponseEntity<AuthResponse> logout(@RequestBody LogoutRequest request) {
                logger.info("Logout request received for userId: {}", request.getUserId());

                try {
                        String userId = request.getUserId();

                        // Validate token
                        if (request.getToken() != null && !jwtTokenProvider.validateToken(request.getToken())) {
                                logger.warn("Invalid token provided for logout");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                                new AuthResponse("error", "Invalid or expired token", null, null, null,
                                                                null, null, null, null,
                                                                null));
                        }

                        // Log the logout action
                        auditLogService.logUserAction(userId, null, null, "LOGOUT", "SUCCESS", null);

                        logger.info("User logged out successfully: {}", userId);
                        return ResponseEntity.ok(new AuthResponse(
                                        "success",
                                        "Logout successful",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null));

                } catch (Exception e) {
                        logger.error("Error during logout: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        new AuthResponse("error", "Logout failed: " + e.getMessage(), null, null, null,
                                                        null, null, null,
                                                        null, null));
                }
        }
}
