package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.lauriel.store.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull OrderStatus status) {
    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
