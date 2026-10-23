package com.lauriel.store.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "purchase_receipt_details")
@Getter
@Setter
public class PurchaseReceiptDetail {

    @EmbeddedId
    private PurchaseReceiptDetailId id = new PurchaseReceiptDetailId();

    @MapsId("receiptId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private PurchaseReceipt purchaseReceipt;

    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", precision = 14, scale = 2, nullable = false)
    private BigDecimal subtotal;
}
