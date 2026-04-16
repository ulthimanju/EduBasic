package com.app.auth.user.management.service;

import com.app.auth.cache.service.CacheService;
import com.app.auth.common.exception.UserNotFoundException;
import com.app.auth.user.dto.UserResponseDTO;
import com.app.auth.user.mapper.UserMapper;
import com.app.auth.user.node.UserNode;
import com.app.auth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation for {@link UserManagementService}.
 */
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CacheService cacheService;

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
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found: " + userId);
        }

        userRepository.deleteById(userId);
        cacheService.evictUserCache(userId);
    }
}
