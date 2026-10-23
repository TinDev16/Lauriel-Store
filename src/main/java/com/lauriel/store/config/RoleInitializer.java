package com.lauriel.store.config;

import com.lauriel.store.entity.Role;
import com.lauriel.store.repository.RoleRepository;
import com.lauriel.store.security.StoreRole;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
@ConditionalOnProperty(
    name = "app.roles.initialize",
    havingValue = "true",
    matchIfMissing = true
)
public class RoleInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (StoreRole roleName : StoreRole.values()) {
            if (roleRepository.findByRoleName(roleName.name()).isEmpty()) {
                Role role = new Role();
                role.setRoleName(roleName.name());
                roleRepository.save(role);
            }
        }
    }
}
