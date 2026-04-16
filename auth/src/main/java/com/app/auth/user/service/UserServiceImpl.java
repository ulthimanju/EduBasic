package com.app.auth.user.service;

import com.app.auth.user.node.UserNode;
import com.app.auth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements {@link UserService} using Spring Data Neo4j.
 *
 * <p>Atomic rule: each method calls exactly one repository method and
 * performs one logical operation only.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Algorithm (§9.2 of design doc):
     * <ol>
     *   <li>findByGoogleId — check for existing user</li>
     *   <li>NOT found → create new node with createdAt + lastLogin = now</li>
     *   <li>FOUND → update name + lastLogin, save</li>
     * </ol>
     * NOTE: googleId is the merge key; email is updated but not used as a key.
     */
    @Override
    @Transactional
    public UserNode upsertUser(String googleId, String email, String name) {
        return userRepository.findByGoogleId(googleId)
                .map(existing -> updateExistingUser(existing, name))
                .orElseGet(() -> createNewUser(googleId, email, name));
    }

    @Override
    public Optional<UserNode> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<UserNode> findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private UserNode createNewUser(String googleId, String email, String name) {
        LocalDateTime now = LocalDateTime.now();
        UserNode newUser = UserNode.builder()
                .id(UUID.randomUUID().toString())
                .googleId(googleId)
                .email(email)
                .name(name)
                .createdAt(now)
                .lastLogin(now)
                .build();
        UserNode saved = userRepository.save(newUser);
        log.info("New user created: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    private UserNode updateExistingUser(UserNode existing, String name) {
        existing.setName(name);
        existing.setLastLogin(LocalDateTime.now());
        UserNode saved = userRepository.save(existing);
        log.debug("Returning user updated: id={}", saved.getId());
        return saved;
    }
}
