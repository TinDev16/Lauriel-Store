package com.lauriel.store.dto.response;

import java.math.BigDecimal;
import com.lauriel.store.entity.enums.ActiveStatus;

public record ProductSummaryResponse(Long productId, String productName, BigDecimal price, String imgUrl, ActiveStatus status) {
}
