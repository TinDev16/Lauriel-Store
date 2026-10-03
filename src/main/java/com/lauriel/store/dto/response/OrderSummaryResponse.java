package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(Long orderId, Long customerId, OrderStatus status, LocalDateTime createdAt,
                                   BigDecimal totalAmount, BigDecimal orderPromotionDiscount, BigDecimal discountAmount,
                                   BigDecimal shippingFee, BigDecimal finalAmount) {
}
