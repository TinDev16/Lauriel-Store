package com.lauriel.store.dto.store;

import com.lauriel.store.dto.response.ColorResponse;
import com.lauriel.store.dto.response.PromotionInfoResponse;
import com.lauriel.store.dto.response.SizeResponse;
import java.math.BigDecimal;

public record StoreCartItemView(
    Long productId,
    Long variantId,
    String productName,
    String imageUrl,
    String sku,
    SizeResponse size,
    ColorResponse color,
    BigDecimal originalPrice,
    BigDecimal effectivePrice,
    PromotionInfoResponse promotion,
    int quantity,
    int stock,
    BigDecimal subtotal
) {}
