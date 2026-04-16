package com.app.auth.user.service;

import com.app.auth.user.node.UserNode;

import java.util.Optional;

/**
 * Contract for user persistence operations.
 * Callers depend on this interface — not on the implementation.
 */
public interface UserService {

    /**
     * Create or update the user identified by {@code googleId}.
     * On first login a new node is created. On subsequent logins lastLogin is updated.
     *
     * @return the persisted (or updated) UserNode — never null
     */
    UserNode upsertUser(String googleId, String email, String name);

    /**
     * Find a user by their internal UUID (used after JWT extraction).
     */
    Optional<UserNode> findById(String id);

    /**
     * Find a user by Google's stable "sub" claim.
     */
    Optional<UserNode> findByGoogleId(String googleId);
}
