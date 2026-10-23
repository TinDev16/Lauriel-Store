package com.lauriel.store.dto.response;

import java.math.BigDecimal;

public record OrderDetailResponse(
    Long variantId,
    String sku,
    String productName,
    SizeResponse size,
    ColorResponse color,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal discountAmount,
    BigDecimal subtotal,
    String imageUrl
) {}
