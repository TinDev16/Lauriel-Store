package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

public record SupplierRequest(
    @NotBlank @Size(max = 200) String supplierName,
    @Size(max = 20) String phone,
    @Email @Size(max = 150) String email,
    String address,
    @NotNull ActiveStatus status
) {
    @AssertTrue(message = "address vượt quá giới hạn TEXT")
    @JsonIgnore
    public boolean isAddressWithinStorageLimit() {
        return (
            address == null ||
            address.getBytes(StandardCharsets.UTF_8).length <= 65535
        );
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }
}
