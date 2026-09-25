package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(Long productId, String productName, CategorySummaryResponse category,
                                    String description, BigDecimal basePrice, String imgUrl,
                                    ActiveStatus status, List<ProductVariantResponse> variants,
                                    List<ProductImageResponse> images) {
    public ProductDetailResponse(Long productId, String productName, CategorySummaryResponse category,
            String description, BigDecimal basePrice, String imgUrl, ActiveStatus status,
            List<ProductVariantResponse> variants) {
        this(productId, productName, category, description, basePrice, imgUrl, status, variants, List.of());
    }
}
