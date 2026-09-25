package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;

public record ProductResponse(Long productId, String productName, CategorySummaryResponse category,
                              String description, BigDecimal price, String imgUrl, ActiveStatus status) {
}
