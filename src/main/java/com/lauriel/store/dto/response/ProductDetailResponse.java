package com.lauriel.store.dto.response;

import java.math.BigDecimal;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.util.List;

public record ProductDetailResponse(Long productId, String productName, String description, BigDecimal basePrice,
                                    String imgUrl, ActiveStatus status, List<ProductVariantResponse> variants) {
}
