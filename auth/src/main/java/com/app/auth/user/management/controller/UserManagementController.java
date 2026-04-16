package com.app.auth.user.management.controller;

import com.app.auth.common.response.ApiResponse;
import com.app.auth.user.dto.UserResponseDTO;
import com.app.auth.user.management.dto.UpdateUserRequestDTO;
import com.app.auth.user.management.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User management endpoints.
 */
@RestController
@RequestMapping("/api/user-management")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.getAllUsers()));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.getUserById(userId)));
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserName(
            @PathVariable String userId,
            @RequestBody UpdateUserRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(
                userManagementService.updateUserName(userId, request.name())));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        userManagementService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("User deleted"));
    }
}
