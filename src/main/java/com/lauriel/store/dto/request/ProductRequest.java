package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public record ProductRequest(
        @NotBlank @Size(max = 200) String productName,
        @NotNull @Positive Long categoryId,
        String description,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal price,
        String imgUrl,
        @NotNull ActiveStatus status) {

    @AssertTrue(message = "description/imgUrl vượt quá giới hạn TEXT")
    @JsonIgnore
    public boolean isTextWithinStorageLimits() {
        return (description == null || description.getBytes(StandardCharsets.UTF_8).length <= 65535)
                && (imgUrl == null || imgUrl.getBytes(StandardCharsets.UTF_8).length <= 65535);
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
