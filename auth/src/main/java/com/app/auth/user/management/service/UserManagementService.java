package com.app.auth.user.management.service;

import com.app.auth.user.dto.UserResponseDTO;

import java.util.List;

/**
 * User management operations for administrative-style user lifecycle tasks.
 */
public interface UserManagementService {

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(String userId);

    UserResponseDTO updateUserName(String userId, String name);

    void deleteUser(String userId);
}
