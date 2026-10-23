package com.lauriel.store.dto.response;

import java.time.LocalDateTime;

public record CustomerAccountProfileResponse(
    Long customerId,
    String customerName,
    String phone,
    String address,
    LocalDateTime createdAt
) {}
