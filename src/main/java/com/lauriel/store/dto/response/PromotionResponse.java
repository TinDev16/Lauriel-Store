package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionResponse(
    Long promotionId,
    String promotionName,
    String description,
    BigDecimal discountPercent,
    LocalDateTime startDate,
    LocalDateTime endDate,
    ActiveStatus status
) {}
