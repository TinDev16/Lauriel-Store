package com.lauriel.store.dto.response;

import java.math.BigDecimal;
import com.lauriel.store.entity.enums.ActiveStatus;

public record ProductVariantResponse(Long variantId, Long productId, String sku, SizeResponse size, ColorResponse color,
                                     BigDecimal price, Integer quantity, ActiveStatus status,
                                     BigDecimal effectivePrice, PromotionInfoResponse promotion) {
    public ProductVariantResponse(Long variantId, Long productId, String sku, SizeResponse size, ColorResponse color,
            BigDecimal price, Integer quantity, ActiveStatus status) {
        this(variantId, productId, sku, size, color, price, quantity, status, price, null);
    }
}
