package com.lauriel.store.dto.response;

import java.util.List;

public record ProductInventoryDetailResponse(
    ProductInventorySummaryResponse summary,
    List<InventoryResponse> variants
) {}
