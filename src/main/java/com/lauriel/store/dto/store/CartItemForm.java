package com.lauriel.store.dto.store;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemForm(@NotNull @Positive Long variantId,
        @NotNull @Positive @Max(1000) Integer quantity) {}
