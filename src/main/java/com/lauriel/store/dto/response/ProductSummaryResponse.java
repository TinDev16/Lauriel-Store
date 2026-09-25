package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;

public record ProductSummaryResponse(Long productId, String productName, CategorySummaryResponse category,
                                     BigDecimal price, String imgUrl, ActiveStatus status) {
}
