package com.edubas.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.edubas.backend.entity.User;
import com.edubas.backend.repository.UserRepository;
import com.edubas.backend.util.AvatarGenerator;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public User registerUser(String username, String email, String password, String role) {
        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            logger.warn("Registration failed: Username '{}' already exists", username);
            auditLogService.logFailedLogin(username, "Username already exists");
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            logger.warn("Registration failed: Email '{}' already exists", email);
            auditLogService.logFailedLogin(email, "Email already exists");
            throw new IllegalArgumentException("Email already exists");
        }

        // Hash password using BCrypt
        String hashedPassword = passwordEncoder.encode(password);

        // Generate avatar SVG
        String avatarSvg = AvatarGenerator.generateAvatarSVG(username, email);

        // Create and save new user
        User user = new User(username, email, hashedPassword, role);
        user.setAvatar(avatarSvg);
        User savedUser = userRepository.save(user);

        // Log successful registration
        auditLogService.logUserAction(savedUser, "REGISTER", "SUCCESS");
        logger.info("User registered successfully: {} ({}) with generated avatar", username, email);
        return savedUser;
    }

    public User loginUser(String usernameOrEmail, String password) {
        // Try to find user by username or email
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> {
                    logger.warn("Login failed: User '{}' not found", usernameOrEmail);
                    auditLogService.logFailedLogin(usernameOrEmail, "User not found");
                    return new IllegalArgumentException("Invalid username/email or password");
                });

        // Verify password using BCrypt
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.warn("Login failed: Invalid password for user '{}'", usernameOrEmail);
            auditLogService.logFailedLogin(usernameOrEmail, "Invalid password");
            throw new IllegalArgumentException("Invalid username/email or password");
        }

        // Log successful login
        auditLogService.logUserAction(user, "LOGIN", "SUCCESS");
        logger.info("User logged in successfully: {} ({})", user.getUsername(), user.getEmail());
        return user;
    }

    public void changePassword(String userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            auditLogService.logUserAction(user, "PASSWORD_CHANGE", "FAILURE", "Old password incorrect");
            throw new IllegalArgumentException("Old password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditLogService.logUserAction(user, "PASSWORD_CHANGE", "SUCCESS");
        logger.info("Password changed for user: {}", user.getUsername());
    }

    public User updateProfileSettings(String userId, Boolean profileVisibility, Boolean emailNotifications) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update settings if provided
        if (profileVisibility != null) {
            user.setProfileVisibility(profileVisibility);
        }
        if (emailNotifications != null) {
            user.setEmailNotifications(emailNotifications);
        }

        User updatedUser = userRepository.save(user);
        auditLogService.logUserAction(user, "PROFILE_SETTINGS_UPDATE", "SUCCESS");
        logger.info("Profile settings updated for user: {}", user.getUsername());
        return updatedUser;
    }

    public User getUserById(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getUserByUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
