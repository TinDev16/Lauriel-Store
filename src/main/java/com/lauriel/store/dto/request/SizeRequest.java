package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record SizeRequest(@NotBlank @Size(max = 20) String sizeName) {

    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
