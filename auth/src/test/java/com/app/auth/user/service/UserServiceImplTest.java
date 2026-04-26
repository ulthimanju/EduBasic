package com.app.auth.user.service;

import com.app.auth.common.config.AdminProperties;
import com.app.auth.common.exception.EmailConflictException;
import com.app.auth.cache.service.CacheService;
import com.app.auth.user.node.AppRole;
import com.app.auth.user.node.UserNode;
import com.app.auth.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AdminProperties adminProperties;
    @Mock private CacheService   cacheService;

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
                        .build());

        UserNode result = userService.upsertUser(GOOGLE_ID, EMAIL, NAME);

        assertThat(result.getRoles()).containsExactly(AppRole.STUDENT);
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
    @DisplayName("upsertUser — email conflict: throws EmailConflictException if email taken by different googleId")
    void upsertUser_emailConflict_throwsException() {
        UserNode otherUser = UserNode.builder()
                .googleId("other-google-id")
                .email(EMAIL)
                .build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.upsertUser(GOOGLE_ID, EMAIL, NAME))
                .isInstanceOf(EmailConflictException.class)
                .hasMessageContaining("already associated with a different account");
    }

    @Test
    @DisplayName("syncRoles — roles changed: updates DB and evicts cache")
    void syncRoles_rolesChanged_updatesAndEvicts() {
        UserNode user = UserNode.builder()
                .id("u1")
                .email(EMAIL)
                .roles(new HashSet<>(Set.of(AppRole.STUDENT)))
                .build();

        when(adminProperties.getAllowedEmailSet()).thenReturn(Set.of(EMAIL));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserNode result = userService.syncRoles(user);

        assertThat(result.getRoles()).containsExactlyInAnyOrder(AppRole.STUDENT, AppRole.ADMIN);
        verify(userRepository).save(user);
        verify(cacheService).evictUserCache("u1");
    }

    @Test
    @DisplayName("syncRoles — roles unchanged: does nothing")
    void syncRoles_noChange_doesNothing() {
        UserNode user = UserNode.builder()
                .id("u1")
                .email(EMAIL)
                .roles(new HashSet<>(Set.of(AppRole.STUDENT)))
                .build();

        when(adminProperties.getAllowedEmailSet()).thenReturn(Set.of());

        UserNode result = userService.syncRoles(user);

        assertThat(result).isSameAs(user);
        verifyNoInteractions(userRepository, cacheService);
    }

    private Set<AppRole> mapToAppRoles(Set<String> roles) {
        return roles.stream().map(AppRole::valueOf).collect(Collectors.toSet());
    }
}
