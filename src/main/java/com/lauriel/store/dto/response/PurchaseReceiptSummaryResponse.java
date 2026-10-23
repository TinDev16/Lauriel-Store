package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.PurchaseReceiptStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseReceiptSummaryResponse(
    Long receiptId,
    SupplierResponse supplier,
    ReceiptActorResponse createdBy,
    PurchaseReceiptStatus status,
    LocalDateTime createdAt,
    BigDecimal totalAmount
) {}
