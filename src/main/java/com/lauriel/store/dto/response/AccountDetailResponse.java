package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.security.StoreRole;
import java.time.LocalDateTime;

public record AccountDetailResponse(Long userId, String username, String fullName, String email,
        String phone, StoreRole role, UserStatus status, LocalDateTime createdAt,
        CustomerAccountProfileResponse customerProfile) {
}
