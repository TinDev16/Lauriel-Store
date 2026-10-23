package com.lauriel.store.dto.request;

import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.security.StoreRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
    @NotBlank @Size(max = 150) String fullName,
    @Email @Size(max = 150) String email,
    @Size(max = 20) String phone,
    @NotNull StoreRole role,
    @NotNull UserStatus status
) {}
