package com.app.auth.user.service;

import com.app.auth.common.config.AdminProperties;
import com.app.auth.user.node.UserNode;
import com.app.auth.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserServiceImpl}.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>New users are created with all fields set correctly</li>
 *   <li>Returning users have email, name, AND lastLogin updated (email drift fix)</li>
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
    private static final String EMAIL_OLD  = "old@example.com";
    private static final String EMAIL_NEW  = "new@example.com";
    private static final String NAME_NEW   = "Updated Name";
    private static final String USER_ID    = "internal-uuid-456";

    @Test
    @DisplayName("upsertUser — new user: creates node with all fields populated")
    void upsertUser_newUser_createsWithAllFields() {
        when(adminProperties.getAllowedEmailSet()).thenReturn(Set.of());
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserNode.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        UserNode result = userService.upsertUser(GOOGLE_ID, EMAIL_NEW, NAME_NEW);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(result.getGoogleId()).isEqualTo(GOOGLE_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL_NEW);
        assertThat(result.getName()).isEqualTo(NAME_NEW);
        assertThat(result.getId()).isNotBlank();
        assertThat(result.getCreatedAt()).isBetween(before, after);
        assertThat(result.getLastLogin()).isBetween(before, after);
    }

    @Test
    @DisplayName("upsertUser — existing user: updates email, name, and lastLogin (email drift fix)")
    void upsertUser_existingUser_updatesEmailNameAndLastLogin() {
        UserNode existing = UserNode.builder()
                .id(USER_ID)
                .googleId(GOOGLE_ID)
                .email(EMAIL_OLD)
                .name("Old Name")
                .createdAt(LocalDateTime.now().minusDays(10))
                .lastLogin(LocalDateTime.now().minusDays(1))
                .build();

        when(adminProperties.getAllowedEmailSet()).thenReturn(Set.of());
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(UserNode.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<UserNode> captor = ArgumentCaptor.forClass(UserNode.class);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        userService.upsertUser(GOOGLE_ID, EMAIL_NEW, NAME_NEW);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        verify(userRepository).save(captor.capture());
        UserNode saved = captor.getValue();

        // Email must be updated (core of the email drift fix)
        assertThat(saved.getEmail()).isEqualTo(EMAIL_NEW);
        assertThat(saved.getName()).isEqualTo(NAME_NEW);
        assertThat(saved.getLastLogin()).isBetween(before, after);

        // ID and googleId must not change
        assertThat(saved.getId()).isEqualTo(USER_ID);
        assertThat(saved.getGoogleId()).isEqualTo(GOOGLE_ID);
    }
}
