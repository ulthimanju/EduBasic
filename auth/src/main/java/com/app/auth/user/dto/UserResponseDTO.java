package com.app.auth.user.dto;

import com.app.auth.user.node.AppRole;
import java.util.Set;

/**
 * Safe outbound representation of an authenticated user.
 * Never includes internal IDs, passwords, or raw graph properties.
 */
public record UserResponseDTO(String id, String email, String name, Set<AppRole> roles) {}
