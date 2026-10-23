package com.lauriel.store.dto.request;

import com.lauriel.store.security.StoreRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull StoreRole role) {}
