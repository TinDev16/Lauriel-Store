package com.lauriel.store.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

public record LoginRequest(
    @NotBlank @Size(max = 100) String username,
    @NotBlank @Size(max = 72) String password
) {
    @AssertTrue(message = "Mật khẩu không được vượt quá 72 byte UTF-8")
    @JsonIgnore
    public boolean isPasswordWithinBcryptLimit() {
        return (
            password == null ||
            password.getBytes(StandardCharsets.UTF_8).length <= 72
        );
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Trường dữ liệu không được hỗ trợ");
    }

    @Override
    public String toString() {
        return "LoginRequest[credentials redacted]";
    }
}
