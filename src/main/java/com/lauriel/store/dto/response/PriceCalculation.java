package com.lauriel.store.dto.response;

import java.math.BigDecimal;

public record PriceCalculation(BigDecimal basePrice, BigDecimal effectivePrice, BigDecimal promotionDiscount,
                               PromotionInfoResponse promotion) {
}
