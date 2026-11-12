package com.lauriel.store.dto.response;

import java.math.BigDecimal;

public record OrderPromotionSuggestion(
    Long orderPromotionId,
    String promotionName,
    String description,
    BigDecimal minOrderAmount,
    BigDecimal discountPercent,
    BigDecimal maxDiscountAmount,
    BigDecimal currentSubtotal,
    BigDecimal remainingAmount,
    int progressPercent
) {}

