package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.time.LocalDateTime;

public record SupplierResponse(
    Long supplierId,
    String supplierName,
    String phone,
    String email,
    String address,
    ActiveStatus status,
    LocalDateTime createdAt
) {}
