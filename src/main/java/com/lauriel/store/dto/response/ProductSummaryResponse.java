package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;

public record ProductSummaryResponse(
    Long productId,
    String productName,
    CategorySummaryResponse category,
    BigDecimal price,
    String imgUrl,
    ActiveStatus status,
    BigDecimal effectivePrice,
    PromotionInfoResponse promotion,
    Long totalStock
) {
    public ProductSummaryResponse(
        Long productId,
        String productName,
        CategorySummaryResponse category,
        BigDecimal price,
        String imgUrl,
        ActiveStatus status,
        BigDecimal effectivePrice,
        PromotionInfoResponse promotion
    ) {
        this(
            productId,
            productName,
            category,
            price,
            imgUrl,
            status,
            effectivePrice,
            promotion,
            null
        );
    }

    public ProductSummaryResponse(
        Long productId,
        String productName,
        CategorySummaryResponse category,
        BigDecimal price,
        String imgUrl,
        ActiveStatus status
    ) {
        this(
            productId,
            productName,
            category,
            price,
            imgUrl,
            status,
            price,
            null,
            null
        );
    }

    public boolean isOutOfStock() {
        return totalStock != null && totalStock <= 0;
    }

    public boolean isInStock() {
        return totalStock == null || totalStock > 0;
    }

    public String imageUrl() {
        return imgUrl;
    }
}
