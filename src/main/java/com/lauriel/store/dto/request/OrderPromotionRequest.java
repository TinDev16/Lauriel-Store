package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

public record OrderPromotionRequest(
        @NotBlank @Size(max=200) String promotionName,
        String description,
        @NotNull @PositiveOrZero @Digits(integer=12, fraction=2) BigDecimal minOrderAmount,
        @NotNull @Positive @DecimalMax("100") @Digits(integer=3, fraction=2) BigDecimal discountPercent,
        @Positive @Digits(integer=12, fraction=2) BigDecimal maxDiscountAmount,
        @NotNull LocalDateTime startDate,
        @NotNull LocalDateTime endDate,
        @NotNull ActiveStatus status) {
    @AssertTrue(message="Thời gian bắt đầu phải trước thời gian kết thúc") @JsonIgnore
    public boolean isTimeRangeValid() { return startDate == null || endDate == null || startDate.isBefore(endDate); }
    @AssertTrue(message="Mô tả vượt quá giới hạn TEXT") @JsonIgnore
    public boolean isDescriptionWithinStorageLimit(){return description==null||description.getBytes(StandardCharsets.UTF_8).length<=65535;}
}
