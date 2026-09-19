package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record PurchaseReceiptCreateRequest(@NotNull @Positive Long supplierId) {
    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
