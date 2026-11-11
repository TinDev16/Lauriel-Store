package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;

public record ProductInventorySummaryResponse(
    Long productId,
    String productName,
    String imageUrl,
    String representativeSku,
    long variantCount,
    long totalQuantity,
    long outOfStockCount,
    long lowStockCount,
    ActiveStatus status
) {}
