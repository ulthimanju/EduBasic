package com.edubas.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.edubas.backend.dto.ProfileSettingsRequest;
import com.edubas.backend.dto.ProfileSettingsResponse;
import com.edubas.backend.entity.User;
import com.edubas.backend.service.UserService;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

        private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
        private final UserService userService;

        public ProfileController(UserService userService) {
                this.userService = userService;
        }

        @PostMapping("/settings")
        public ResponseEntity<ProfileSettingsResponse> updateProfileSettings(
                        @RequestBody ProfileSettingsRequest request) {
                logger.info("Profile settings update request received for userId: {}", request.getUserId());
                logger.debug("Settings update details - Profile Visibility: {}, Email Notifications: {}",
                                request.getProfileVisibility(), request.getEmailNotifications());

                try {
                        User user = userService.updateProfileSettings(
                                        request.getUserId(),
                                        request.getProfileVisibility(),
                                        request.getEmailNotifications());

                        ProfileSettingsResponse response = new ProfileSettingsResponse(
                                        "success",
                                        "Profile settings updated successfully",
                                        user.getProfileVisibility(),
                                        user.getEmailNotifications());

                        logger.info("Profile settings updated successfully for user: {} (ID: {})", user.getUsername(),
                                        user.getId());
                        logger.info("Updated settings - Profile Visibility: {}, Email Notifications: {}",
                                        user.getProfileVisibility(), user.getEmailNotifications());
                        return ResponseEntity.ok(response);

                } catch (IllegalArgumentException e) {
                        logger.warn("Profile settings update failed for userId: {} - Error: {}", request.getUserId(),
                                        e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                                        new ProfileSettingsResponse("error", e.getMessage(), null, null));
                } catch (Exception e) {
                        logger.error("Error during profile settings update for userId: {}", request.getUserId(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        new ProfileSettingsResponse("error",
                                                        "Profile settings update failed: " + e.getMessage(), null,
                                                        null));
                }
        }

        @GetMapping("/settings")
        public ResponseEntity<ProfileSettingsResponse> getProfileSettings(@RequestParam String userId) {
                logger.info("Profile settings fetch request received for userId: {}", userId);

                try {
                        User user = userService.getUserById(userId);

                        ProfileSettingsResponse response = new ProfileSettingsResponse(
                                        "success",
                                        "Profile settings retrieved successfully",
                                        user.getProfileVisibility(),
                                        user.getEmailNotifications());

                        logger.info("Profile settings retrieved successfully for user: {} (ID: {})", user.getUsername(),
                                        user.getId());
                        logger.debug("Current settings - Profile Visibility: {}, Email Notifications: {}",
                                        user.getProfileVisibility(), user.getEmailNotifications());
                        return ResponseEntity.ok(response);

                } catch (IllegalArgumentException e) {
                        logger.warn("Profile settings fetch failed for userId: {} - Error: {}", userId, e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                                        new ProfileSettingsResponse("error", e.getMessage(), null, null));
                } catch (Exception e) {
                        logger.error("Error during profile settings fetch for userId: {}", userId, e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        new ProfileSettingsResponse("error",
                                                        "Profile settings fetch failed: " + e.getMessage(), null,
                                                        null));
                }
        }
}
