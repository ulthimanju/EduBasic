package com.app.auth.user.service;

import com.app.auth.common.config.AdminProperties;
import com.app.auth.LogMessages;
import com.app.auth.common.config.AdminProperties;
import com.app.auth.user.node.AppRole;
import com.app.auth.user.node.UserNode;
import com.app.auth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
    private final AdminProperties adminProperties;

    /**
     * {@inheritDoc}
     *
     * <p>Algorithm (§9.2 of design doc):
     * <ol>
     *   <li>findByGoogleId — check for existing user</li>
     *   <li>NOT found → create new node with createdAt + lastLogin = now</li>
     *   <li>FOUND → update email + name + lastLogin, save</li>
     * </ol>
     * NOTE: googleId is the merge key; email is also updated on each login to
     * capture Google email changes (OAuth email drift).
     */
    @Override
    @Transactional
    public UserNode upsertUser(String googleId, String email, String name) {
        return userRepository.findByGoogleId(googleId)
                .map(existing -> updateExistingUser(existing, email, name))
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
        Set<AppRole> roles = new HashSet<>();
        roles.add(AppRole.STUDENT);

        if (isAdmin(email)) {
            roles.add(AppRole.ADMIN);
        }

        UserNode newUser = UserNode.builder()
                .id(UUID.randomUUID().toString())
                .googleId(googleId)
                .email(email)
                .name(name)
                .roles(roles)
                .createdAt(now)
                .lastLogin(now)
                .build();
        UserNode saved = userRepository.save(newUser);
        log.info(LogMessages.NEW_USER_CREATED, saved.getId(), saved.getEmail(), saved.getRoles());
        return saved;
    }

    private UserNode updateExistingUser(UserNode existing, String email, String name) {
        existing.setEmail(email);
        existing.setName(name);
        existing.setLastLogin(LocalDateTime.now());

        // Sync admin role based on latest allowlist
        if (isAdmin(email)) {
            existing.getRoles().add(AppRole.ADMIN);
        } else {
            existing.getRoles().remove(AppRole.ADMIN);
        }

        // Ensure STUDENT is always present
        existing.getRoles().add(AppRole.STUDENT);

        UserNode saved = userRepository.save(existing);
        log.debug(LogMessages.RETURNING_USER_UPDATED, saved.getId(), saved.getEmail(), saved.getRoles());
        return saved;
    }

    private boolean isAdmin(String email) {
        return adminProperties.getAllowedEmailSet().contains(email.toLowerCase());
    }
}
