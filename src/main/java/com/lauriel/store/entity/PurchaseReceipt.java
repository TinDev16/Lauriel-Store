package com.lauriel.store.entity;

import com.lauriel.store.entity.enums.PurchaseReceiptStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "purchase_receipts")
@Getter
@Setter
public class PurchaseReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long receiptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PurchaseReceiptStatus status = PurchaseReceiptStatus.DRAFT;

    // MySQL owns this timestamp; Hibernate reads the generated value back.
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", columnDefinition = "datetime", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "purchaseReceipt", fetch = FetchType.LAZY)
    private List<PurchaseReceiptDetail> details = new ArrayList<>();

}
