package com.lauriel.store.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lauriel.store.entity.Role;
import com.lauriel.store.repository.RoleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

class RoleInitializerTest {
    @Test
    void insertsOnlyMissingRolesWithoutModifyingExistingCustomerRole() {
        RoleRepository repository = mock(RoleRepository.class);
        Role existing = new Role();
        existing.setRoleId(99L);
        existing.setRoleName("CUSTOMER");
        when(repository.findByRoleName("CUSTOMER")).thenReturn(Optional.of(existing));

        new RoleInitializer(repository).run(new DefaultApplicationArguments());

        var captor = ArgumentCaptor.forClass(Role.class);
        verify(repository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues().stream().map(Role::getRoleName))
                .containsExactly("SUPER_ADMIN", "ADMIN", "STAFF");
        assertThat(existing.getRoleId()).isEqualTo(99L);
        assertThat(existing.getRoleName()).isEqualTo("CUSTOMER");
    }

    @Test
    void existingRolesRequireNoWrites() {
        RoleRepository repository = mock(RoleRepository.class);
        for (String name : new String[] {"SUPER_ADMIN", "ADMIN", "STAFF", "CUSTOMER"}) {
            Role role = new Role();
            role.setRoleName(name);
            when(repository.findByRoleName(name)).thenReturn(Optional.of(role));
        }
        new RoleInitializer(repository).run(new DefaultApplicationArguments());
        verify(repository, never()).save(any());
    }
}
