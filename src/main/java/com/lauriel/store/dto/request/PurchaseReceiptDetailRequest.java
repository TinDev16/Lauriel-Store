package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PurchaseReceiptDetailRequest(
    @NotNull @Positive Long variantId,
    @NotNull @Positive Integer quantity,
    @NotNull
    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    BigDecimal unitPrice
) {
    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
