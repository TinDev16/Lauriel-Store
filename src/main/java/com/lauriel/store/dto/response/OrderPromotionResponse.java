package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderPromotionResponse(
    Long orderPromotionId,
    String promotionName,
    String description,
    BigDecimal minOrderAmount,
    BigDecimal discountPercent,
    BigDecimal maxDiscountAmount,
    LocalDateTime startDate,
    LocalDateTime endDate,
    ActiveStatus status
) {}
