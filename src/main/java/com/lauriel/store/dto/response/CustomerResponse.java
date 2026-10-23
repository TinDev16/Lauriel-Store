package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.UserStatus;
import java.time.LocalDateTime;

public record CustomerResponse(
    Long customerId,
    String username,
    String fullName,
    String email,
    String phone,
    UserStatus status,
    LocalDateTime createdAt,
    String address
) {}
