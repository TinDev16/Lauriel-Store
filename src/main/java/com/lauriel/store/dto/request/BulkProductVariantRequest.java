package com.lauriel.store.dto.request;

import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record BulkProductVariantRequest(
    @NotEmpty(message = "Hãy chọn ít nhất một kích cỡ")
    @Size(max = 50)
    List<@Positive Long> sizeIds,
    @NotEmpty(message = "Hãy chọn ít nhất một màu sắc")
    @Size(max = 50)
    List<@Positive Long> colorIds,
    @NotNull
    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    BigDecimal price,
    @NotNull ActiveStatus status
) {}
