package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.validation.constraints.*;

public record ProductStatusRequest(@NotNull ActiveStatus status) {
    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
