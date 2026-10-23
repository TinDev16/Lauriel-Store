package com.lauriel.store.security;

import com.lauriel.store.entity.User;
import com.lauriel.store.entity.enums.UserStatus;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class StoreUserDetails
    extends org.springframework.security.core.userdetails.User
{

    private final Long userId;
    private final StoreRole role;

    public StoreUserDetails(User user) {
        this(
            user,
            StoreRole.valueOf(
                user.getRole().getRoleName().toUpperCase(Locale.ROOT)
            )
        );
    }

    private StoreUserDetails(User user, StoreRole role) {
        super(
            user.getUsername(),
            user.getPasswordHash(),
            user.getStatus() == UserStatus.ACTIVE,
            true,
            true,
            user.getStatus() != UserStatus.LOCKED,
            List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        this.userId = user.getUserId();
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public StoreRole getRole() {
        return role;
    }
}
