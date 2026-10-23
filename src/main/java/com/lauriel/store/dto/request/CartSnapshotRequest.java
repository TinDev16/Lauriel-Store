package com.lauriel.store.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CartSnapshotRequest(
    @NotNull @Size(max = 100) List<@Valid CheckoutItemRequest> items
) {}
