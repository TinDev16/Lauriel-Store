package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;

public record ProductVariantRequest(@NotNull @Positive Long sizeId,
                                    @NotNull @Positive Long colorId,
                                    @NotBlank @Size(max = 100) String sku,
                                    @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal price,
                                    @NotNull ActiveStatus status) {

    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
