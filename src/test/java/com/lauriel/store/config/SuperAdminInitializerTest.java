package com.lauriel.store.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lauriel.store.entity.Role;
import com.lauriel.store.entity.User;
import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.repository.RoleRepository;
import com.lauriel.store.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SuperAdminInitializerTest {
    @Test
    void createsInitialSuperAdminWithBcryptWhenEnvironmentIsValid() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        var encoder = new BCryptPasswordEncoder();
        Role role = new Role(); role.setRoleName("SUPER_ADMIN");
        when(roles.findByRoleName("SUPER_ADMIN")).thenReturn(Optional.of(role));
        when(users.saveAndFlush(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0); user.setUserId(91L); return user;
        });

        new SuperAdminInitializer(users, roles, encoder, "owner", "Owner@123",
                "owner@lauriel.local", "Lauriel Owner").run(new DefaultApplicationArguments());

        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole().getRoleName()).isEqualTo("SUPER_ADMIN");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getPasswordHash()).isNotEqualTo("Owner@123");
        assertThat(encoder.matches("Owner@123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void existingSuperAdminPreventsDuplicateCreation() {
        UserRepository users = mock(UserRepository.class);
        when(users.existsByRole_RoleName("SUPER_ADMIN")).thenReturn(true);

        new SuperAdminInitializer(users, mock(RoleRepository.class), new BCryptPasswordEncoder(),
                "owner", "Owner@123", "owner@lauriel.local", "Lauriel Owner")
                .run(new DefaultApplicationArguments());

        verify(users, never()).saveAndFlush(any());
    }
}
