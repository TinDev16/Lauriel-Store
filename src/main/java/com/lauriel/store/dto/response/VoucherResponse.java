package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.entity.enums.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherResponse(
    Long voucherId,
    String code,
    String voucherName,
    DiscountType discountType,
    BigDecimal discountValue,
    BigDecimal minOrderAmount,
    BigDecimal maxDiscountAmount,
    Integer quantity,
    LocalDateTime startDate,
    LocalDateTime endDate,
    ActiveStatus status
) {}
