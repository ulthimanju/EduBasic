package com.app.auth.user.management.service;

import com.app.auth.cache.service.CacheService;
import com.app.auth.common.exception.UserNotFoundException;
import com.app.auth.session.repository.SessionRepository;
import com.app.auth.user.dto.UserResponseDTO;
import com.app.auth.user.mapper.UserMapper;
import com.app.auth.user.node.AppRole;
import com.app.auth.user.node.UserNode;
import com.app.auth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Default implementation for {@link UserManagementService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CacheService cacheService;
    private final SessionRepository sessionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(String userId) {
        UserNode userNode = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        return userMapper.toResponseDTO(userNode);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUserName(String userId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        UserNode userNode = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        userNode.setName(name.trim());
        UserNode saved = userRepository.save(userNode);

        UserResponseDTO dto = userMapper.toResponseDTO(saved);
        cacheService.cacheUserProfile(userId, dto);
        return dto;
    }

    @Override
    @Transactional
    public UserResponseDTO updateUserRoles(String userId, Set<AppRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Roles set must not be empty");
        }

        UserNode userNode = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        userNode.setRoles(roles);
        UserNode saved = userRepository.save(userNode);

        // Revoke all sessions to force re-login and role claim update
        sessionRepository.revokeAllSessionsForUser(userId)
                .forEach(session -> cacheService.cacheJwtValidity(session.getSessionId(), false));

        UserResponseDTO dto = userMapper.toResponseDTO(saved);
        cacheService.evictUserCache(userId); // Force reload from DB on next request
        cacheService.cacheUserProfile(userId, dto);

        log.info("Updated roles for user {}: {}", userId, roles);
        return dto;
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found: " + userId);
        }

        userRepository.deleteById(userId);
        cacheService.evictUserCache(userId);
    }
}
