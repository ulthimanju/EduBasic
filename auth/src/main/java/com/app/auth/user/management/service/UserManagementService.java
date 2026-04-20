package com.app.auth.user.management.service;

import com.app.auth.user.dto.UserResponseDTO;
import com.app.auth.user.node.AppRole;

import java.util.List;
import java.util.Set;

/**
 * User management operations for administrative-style user lifecycle tasks.
 */
public interface UserManagementService {

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(String userId);

    UserResponseDTO updateUserName(String userId, String name);

    UserResponseDTO updateUserRoles(String userId, Set<AppRole> roles);

    void deleteUser(String userId);
}
