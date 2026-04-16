package com.app.auth.user.dto;

/**
 * Safe outbound representation of an authenticated user.
 * Never includes internal IDs, passwords, or raw graph properties.
 */
public record UserResponseDTO(String id, String email, String name) {}
