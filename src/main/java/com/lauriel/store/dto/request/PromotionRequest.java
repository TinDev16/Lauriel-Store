package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

public record PromotionRequest(@NotBlank @Size(max = 200) String promotionName, String description,
                               @NotNull @Positive @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal discountPercent,
                               @NotNull LocalDateTime startDate, @NotNull LocalDateTime endDate,
                               @NotNull ActiveStatus status) {

    @AssertTrue(message = "description vượt quá giới hạn TEXT")
    @JsonIgnore
    public boolean isDescriptionWithinStorageLimit() {
        return description == null || description.getBytes(StandardCharsets.UTF_8).length <= 65535;
    }

    @AssertTrue(message = "startDate phải trước endDate")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return startDate == null || endDate == null || startDate.isBefore(endDate);
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
