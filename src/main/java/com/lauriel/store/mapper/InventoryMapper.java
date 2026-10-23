package com.lauriel.store.mapper;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.*;
import java.util.List;

public final class InventoryMapper {

    private InventoryMapper() {}

    public static SupplierResponse supplier(Supplier supplier) {
        return new SupplierResponse(
            supplier.getSupplierId(),
            supplier.getSupplierName(),
            supplier.getPhone(),
            supplier.getEmail(),
            supplier.getAddress(),
            supplier.getStatus(),
            supplier.getCreatedAt()
        );
    }

    public static ReceiptActorResponse actor(User user) {
        return user == null
            ? null
            : new ReceiptActorResponse(
                  user.getUserId(),
                  user.getUsername(),
                  user.getFullName()
              );
    }

    public static PurchaseReceiptSummaryResponse summary(
        PurchaseReceipt receipt
    ) {
        return new PurchaseReceiptSummaryResponse(
            receipt.getReceiptId(),
            supplier(receipt.getSupplier()),
            actor(receipt.getUser()),
            receipt.getStatus(),
            receipt.getCreatedAt(),
            receipt.getTotalAmount()
        );
    }

    public static PurchaseReceiptListResponse listItem(
        PurchaseReceipt receipt,
        List<PurchaseReceiptDetail> details
    ) {
        return new PurchaseReceiptListResponse(
            receipt.getReceiptId(),
            supplier(receipt.getSupplier()),
            actor(receipt.getUser()),
            receipt.getStatus(),
            receipt.getCreatedAt(),
            receipt.getTotalAmount(),
            details.stream().map(InventoryMapper::item).toList()
        );
    }

    public static PurchaseReceiptResponse receipt(
        PurchaseReceipt receipt,
        List<PurchaseReceiptDetail> details
    ) {
        return new PurchaseReceiptResponse(
            receipt.getReceiptId(),
            supplier(receipt.getSupplier()),
            actor(receipt.getUser()),
            receipt.getStatus(),
            receipt.getCreatedAt(),
            receipt.getTotalAmount(),
            details.stream().map(InventoryMapper::item).toList()
        );
    }

    public static PurchaseReceiptDetailResponse item(
        PurchaseReceiptDetail detail
    ) {
        var variant = detail.getProductVariant();
        return new PurchaseReceiptDetailResponse(
            variant.getVariantId(),
            variant.getSku(),
            variant.getProduct().getProductName(),
            CatalogMapper.size(variant.getSize()),
            CatalogMapper.color(variant.getColor()),
            detail.getQuantity(),
            detail.getUnitPrice(),
            detail.getSubtotal()
        );
    }

    public static InventoryResponse inventory(ProductVariant variant) {
        return new InventoryResponse(
            variant.getVariantId(),
            variant.getSku(),
            variant.getProduct().getProductId(),
            variant.getProduct().getProductName(),
            variant.getProduct().getPrice(),
            CatalogMapper.size(variant.getSize()),
            CatalogMapper.color(variant.getColor()),
            variant.getPrice(),
            variant.getQuantity(),
            variant.getStatus()
        );
    }

    public static InventoryTransactionResponse history(
        InventoryTransaction transaction
    ) {
        var variant = transaction.getProductVariant();
        return new InventoryTransactionResponse(
            transaction.getTransactionId(),
            variant.getVariantId(),
            variant.getSku(),
            variant.getProduct().getProductName(),
            transaction.getTransactionType(),
            transaction.getQuantity(),
            transaction.getReferenceType(),
            transaction.getReferenceId(),
            actor(transaction.getUser()),
            transaction.getNote(),
            transaction.getCreatedAt()
        );
    }
}
