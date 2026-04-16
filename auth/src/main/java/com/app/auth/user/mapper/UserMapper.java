package com.app.auth.user.mapper;

import com.app.auth.user.dto.UserResponseDTO;
import com.app.auth.user.node.UserNode;
import org.springframework.stereotype.Component;

/**
 * Maps UserNode graph entities to outbound DTOs.
 *
 * <p>One method, one responsibility. Never add business logic here.</p>
 */
@Component
public class UserMapper {

    /**
     * Converts a persisted {@link UserNode} to the safe outbound {@link UserResponseDTO}.
     *
     * @param node the Neo4j node — must not be null
     * @return DTO safe to return in API responses
     */
    public UserResponseDTO toResponseDTO(UserNode node) {
        return new UserResponseDTO(node.getId(), node.getEmail(), node.getName());
    }
}
