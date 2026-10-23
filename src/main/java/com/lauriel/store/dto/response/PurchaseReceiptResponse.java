package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.PurchaseReceiptStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseReceiptResponse(
    Long receiptId,
    SupplierResponse supplier,
    ReceiptActorResponse createdBy,
    PurchaseReceiptStatus status,
    LocalDateTime createdAt,
    BigDecimal totalAmount,
    List<PurchaseReceiptDetailResponse> items
) {}
