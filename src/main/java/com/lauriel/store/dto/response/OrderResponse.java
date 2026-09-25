package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long orderId, Long customerId, OrderStatus status, LocalDateTime createdAt,
                            LocalDateTime updatedAt, String shippingAddress, BigDecimal totalAmount,
                            BigDecimal discountAmount, BigDecimal shippingFee, BigDecimal finalAmount,
                            List<OrderDetailResponse> items, String voucherCode) {
}
