package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;

public record ProductVariantResponse(
    Long variantId,
    Long productId,
    String sku,
    SizeResponse size,
    ColorResponse color,
    BigDecimal price,
    Integer quantity,
    ActiveStatus status,
    BigDecimal effectivePrice,
    PromotionInfoResponse promotion
) {
    public ProductVariantResponse(
        Long variantId,
        Long productId,
        String sku,
        SizeResponse size,
        ColorResponse color,
        BigDecimal price,
        Integer quantity,
        ActiveStatus status
    ) {
        this(
            variantId,
            productId,
            sku,
            size,
            color,
            price,
            quantity,
            status,
            price,
            null
        );
    }
}
