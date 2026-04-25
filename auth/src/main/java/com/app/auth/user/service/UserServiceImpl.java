package com.app.auth.user.service;

import com.app.auth.common.config.AdminProperties;
import com.app.auth.LogMessages;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AdminProperties adminProperties;

    @Override
    @Transactional
    public UserNode upsertUser(String googleId, String email, String name) {
        LocalDateTime now = LocalDateTime.now();
        Set<AppRole> roles = new HashSet<>();
        roles.add(AppRole.STUDENT);
        if (isAdmin(email)) {
            roles.add(AppRole.ADMIN);
        }

        Set<String> roleNames = roles.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        UserNode saved = userRepository.upsertUser(
                UUID.randomUUID().toString(),
                googleId,
                email,
                name,
                roleNames,
                now
        );
        
        log.info("User upserted successfully: {} ({})", saved.getEmail(), saved.getId());
        return saved;
    }

    @Override
    public Optional<UserNode> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<UserNode> findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }

    private boolean isAdmin(String email) {
        return adminProperties.getAllowedEmailSet().contains(email.toLowerCase());
    }
}
