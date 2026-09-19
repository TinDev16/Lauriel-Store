package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import com.lauriel.store.entity.enums.DiscountType;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherRequest(@NotBlank @Size(max = 50) String code, @NotBlank @Size(max = 200) String voucherName,
                             @NotNull DiscountType discountType,
                             @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal discountValue,
                             @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal minOrderAmount,
                             @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal maxDiscountAmount,
                             @NotNull @PositiveOrZero Integer quantity,
                             @NotNull LocalDateTime startDate, @NotNull LocalDateTime endDate,
                             @NotNull ActiveStatus status) {

    @AssertTrue(message = "PERCENT phải không quá 100")
    @JsonIgnore
    public boolean isPercentValid() {
        return discountType != DiscountType.PERCENT || discountValue == null
                || discountValue.compareTo(new BigDecimal("100")) <= 0;
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
