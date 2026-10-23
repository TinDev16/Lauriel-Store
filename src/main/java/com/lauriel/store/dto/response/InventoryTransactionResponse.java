package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.InventoryTransactionType;
import java.time.LocalDateTime;

public record InventoryTransactionResponse(
    Long transactionId,
    Long variantId,
    String sku,
    String productName,
    InventoryTransactionType type,
    Integer quantity,
    String referenceType,
    Long referenceId,
    ReceiptActorResponse performedBy,
    String note,
    LocalDateTime createdAt
) {}
