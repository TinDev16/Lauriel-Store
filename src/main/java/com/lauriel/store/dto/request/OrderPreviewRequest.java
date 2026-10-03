package com.lauriel.store.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record OrderPreviewRequest(@NotEmpty @Size(max=100) @Valid List<@NotNull CheckoutItemRequest> items,
        @Size(max=50) String voucherCode) {}
