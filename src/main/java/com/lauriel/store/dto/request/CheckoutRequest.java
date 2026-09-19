package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public record CheckoutRequest(@NotEmpty @Size(max = 100) @Valid List<@NotNull CheckoutItemRequest> items,
                              @NotBlank String shippingAddress, @Size(max = 50) String voucherCode) {
    public CheckoutRequest(List<CheckoutItemRequest> items, String shippingAddress) {
        this(items, shippingAddress, null);
    }

    @AssertTrue(message = "shippingAddress vượt quá giới hạn TEXT")
    @JsonIgnore
    public boolean isAddressWithinStorageLimit() {
        return shippingAddress == null || shippingAddress.getBytes(StandardCharsets.UTF_8).length <= 65535;
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
