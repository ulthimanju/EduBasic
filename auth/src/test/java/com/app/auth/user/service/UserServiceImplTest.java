package com.app.auth.user.service;

import com.app.auth.common.config.AdminProperties;
import com.app.auth.user.node.AppRole;
import com.app.auth.user.node.UserNode;
import com.app.auth.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserServiceImpl}.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>Users are correctly upserted with calculated roles</li>
 *   <li>Admin promotion/demotion logic works based on config</li>
 * </ul>
 * No Spring context; uses pure Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AdminProperties adminProperties;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String GOOGLE_ID  = "google-sub-123";
    private static final String EMAIL      = "user@example.com";
    private static final String NAME       = "Test User";

    @Test
    @DisplayName("upsertUser — new non-admin user: receives STUDENT role")
    void upsertUser_newStudent_receivesStudentRole() {
        when(adminProperties.getAllowedEmailSet()).thenReturn(Set.of());

        when(userRepository.upsertUser(anyString(), eq(GOOGLE_ID), eq(EMAIL), eq(NAME), anySet(), any()))
                .thenAnswer(inv -> UserNode.builder()
                        .id(inv.getArgument(0))
                        .googleId(inv.getArgument(1))
                        .email(inv.getArgument(2))
                        .name(inv.getArgument(3))
                        .roles(mapToAppRoles(inv.getArgument(4)))
                        .createdAt(inv.getArgument(5))
                        .lastLogin(inv.getArgument(5))
                        .build());

        UserNode result = userService.upsertUser(GOOGLE_ID, EMAIL, NAME);

        assertThat(result.getRoles()).containsExactly(AppRole.STUDENT);
        verify(userRepository).upsertUser(anyString(), eq(GOOGLE_ID), eq(EMAIL), eq(NAME),
                argThat(roles -> roles.contains("STUDENT") && roles.size() == 1), any());
    }

    @Test
    @DisplayName("upsertUser — admin promotion: receives ADMIN role when email is in config")
    void upsertUser_adminPromotion_receivesAdminRole() {
        when(adminProperties.getAllowedEmailSet()).thenReturn(Set.of(EMAIL));

        when(userRepository.upsertUser(anyString(), eq(GOOGLE_ID), eq(EMAIL), eq(NAME), anySet(), any()))
                .thenAnswer(inv -> UserNode.builder()
                        .roles(mapToAppRoles(inv.getArgument(4)))
                        .build());

        UserNode result = userService.upsertUser(GOOGLE_ID, EMAIL, NAME);

        assertThat(result.getRoles()).containsExactlyInAnyOrder(AppRole.STUDENT, AppRole.ADMIN);
    }

    @Test
    @DisplayName("upsertUser — admin demotion: loses ADMIN role when email is removed from config")
    void upsertUser_adminDemotion_losesAdminRole() {
        // config says no admins
        when(adminProperties.getAllowedEmailSet()).thenReturn(Set.of());

        when(userRepository.upsertUser(anyString(), eq(GOOGLE_ID), eq(EMAIL), eq(NAME), anySet(), any()))
                .thenAnswer(inv -> UserNode.builder()
                        .roles(mapToAppRoles(inv.getArgument(4)))
                        .build());

        UserNode result = userService.upsertUser(GOOGLE_ID, EMAIL, NAME);

        // Should only have STUDENT role even if they were admin before (handled by Cypher update)
        assertThat(result.getRoles()).containsExactly(AppRole.STUDENT);
    }

    private Set<AppRole> mapToAppRoles(Set<String> roles) {
        return roles.stream().map(AppRole::valueOf).collect(Collectors.toSet());
    }
}
