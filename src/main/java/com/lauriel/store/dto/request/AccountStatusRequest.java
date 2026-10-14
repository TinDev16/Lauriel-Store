package com.lauriel.store.dto.request;

import com.lauriel.store.entity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull UserStatus status) {
}
