package com.lauriel.store.dto.response;
import java.time.LocalDateTime;
import com.lauriel.store.entity.enums.UserStatus;
public record CustomerResponse(Long customerId, String username, String fullName, String email,
        String phone, UserStatus status, LocalDateTime createdAt, String address) {}
