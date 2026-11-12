package com.lauriel.store.dto.response;

import java.math.BigDecimal;

public record VoucherValidationResponse(
    boolean valid,
    String code,
    String voucherName,
    BigDecimal orderAmount,
    BigDecimal subtotal,
    BigDecimal productPromotionDiscount,
    Long orderPromotionId,
    String orderPromotionName,
    BigDecimal orderPromotionPercent,
    BigDecimal orderPromotionDiscount,
    BigDecimal discountAmount,
    BigDecimal shippingFee,
    BigDecimal finalAmount,
    OrderPromotionSuggestion orderPromotionSuggestion
) {
    public VoucherValidationResponse(
        boolean valid,
        String code,
        String voucherName,
        BigDecimal orderAmount,
        BigDecimal subtotal,
        BigDecimal productPromotionDiscount,
        Long orderPromotionId,
        String orderPromotionName,
        BigDecimal orderPromotionPercent,
        BigDecimal orderPromotionDiscount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal finalAmount
    ) {
        this(
            valid,
            code,
            voucherName,
            orderAmount,
            subtotal,
            productPromotionDiscount,
            orderPromotionId,
            orderPromotionName,
            orderPromotionPercent,
            orderPromotionDiscount,
            discountAmount,
            shippingFee,
            finalAmount,
            null
        );
    }
}
