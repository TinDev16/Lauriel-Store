package com.lauriel.store.dto.response;

import java.math.BigDecimal;

public record VoucherValidationResponse(boolean valid, String code, BigDecimal orderAmount,
                                        BigDecimal discountAmount, BigDecimal shippingFee,
                                        BigDecimal finalAmount) {
}
