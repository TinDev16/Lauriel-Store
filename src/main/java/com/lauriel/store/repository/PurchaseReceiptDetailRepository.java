package com.lauriel.store.repository;

import com.lauriel.store.entity.PurchaseReceiptDetail;
import com.lauriel.store.entity.PurchaseReceiptDetailId;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface PurchaseReceiptDetailRepository
    extends JpaRepository<PurchaseReceiptDetail, PurchaseReceiptDetailId>
{
    @EntityGraph(
        attributePaths = {
            "productVariant.product",
            "productVariant.size",
            "productVariant.color",
        }
    )
    List<PurchaseReceiptDetail> findById_ReceiptIdOrderById_VariantIdAsc(
        Long receiptId
    );

    @EntityGraph(
        attributePaths = {
            "productVariant.product",
            "productVariant.size",
            "productVariant.color",
        }
    )
    @Query(
        "select d from PurchaseReceiptDetail d where d.id.receiptId in :receiptIds " +
            "order by d.id.receiptId, d.id.variantId"
    )
    List<PurchaseReceiptDetail> findListItems(
        java.util.Collection<Long> receiptIds
    );

    // Completion must not preload variant quantities before their write locks are acquired.
    @Query(
        "select d from PurchaseReceiptDetail d where d.id.receiptId = :receiptId order by d.id.variantId"
    )
    List<PurchaseReceiptDetail> findItemsForCompletion(Long receiptId);
}
