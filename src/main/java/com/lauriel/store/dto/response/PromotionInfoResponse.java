package com.lauriel.store.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionInfoResponse(
    Long promotionId,
    String promotionName,
    BigDecimal discountPercent,
    LocalDateTime startDate,
    LocalDateTime endDate
) {}
