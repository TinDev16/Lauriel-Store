package com.lauriel.store.config;

import com.lauriel.store.entity.User;
import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.repository.RoleRepository;
import com.lauriel.store.repository.UserRepository;
import com.lauriel.store.security.StoreRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
public class SuperAdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    private final String email;
    private final String fullName;

    public SuperAdminInitializer(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
            @Value("${app.super-admin.username:}") String username,
            @Value("${app.super-admin.password:}") String password,
            @Value("${app.super-admin.email:}") String email,
            @Value("${app.super-admin.full-name:}") String fullName) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole_RoleName(StoreRole.SUPER_ADMIN.name())) {
            return;
        }
        if (allBlank()) {
            log.info("SUPER_ADMIN bootstrap skipped because environment variables are not configured");
            return;
        }
        validate();
        String normalizedUsername = username.strip();
        String normalizedEmail = email.strip();
        if (users.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalStateException("SUPER_ADMIN_USERNAME đã được sử dụng");
        }
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException("SUPER_ADMIN_EMAIL đã được sử dụng");
        }
        var role = roles.findByRoleName(StoreRole.SUPER_ADMIN.name())
                .orElseThrow(() -> new IllegalStateException("Thiếu role SUPER_ADMIN trong database"));
        User owner = new User();
        owner.setUsername(normalizedUsername);
        owner.setPasswordHash(encoder.encode(password));
        owner.setEmail(normalizedEmail);
        owner.setFullName(fullName.strip());
        owner.setRole(role);
        owner.setStatus(UserStatus.ACTIVE);
        owner = users.saveAndFlush(owner);
        log.info("Created initial SUPER_ADMIN user {}", owner.getUserId());
    }

    private boolean allBlank() {
        return username.isBlank() && password.isBlank() && email.isBlank() && fullName.isBlank();
    }

    private void validate() {
        if (username.isBlank() || username.strip().length() > 100
                || password.length() < 8 || password.length() > 72
                || email.isBlank() || email.strip().length() > 150 || !email.contains("@")
                || fullName.isBlank() || fullName.strip().length() > 150) {
            throw new IllegalStateException("Cấu hình bootstrap SUPER_ADMIN không hợp lệ");
        }
    }
}
