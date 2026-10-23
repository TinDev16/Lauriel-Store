package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;

public record InventoryResponse(
    Long variantId,
    String sku,
    Long productId,
    String productName,
    BigDecimal basePrice,
    SizeResponse size,
    ColorResponse color,
    BigDecimal price,
    Integer quantity,
    ActiveStatus status
) {}
