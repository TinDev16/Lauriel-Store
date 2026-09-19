package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record VoucherValidationRequest(@NotBlank @Size(max = 50) String code,
                                       @NotEmpty @Size(max = 100) @Valid List<@NotNull CheckoutItemRequest> items) {
    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
