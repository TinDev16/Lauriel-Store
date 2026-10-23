package com.lauriel.store.entity;

import com.lauriel.store.entity.enums.InventoryTransactionType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private InventoryTransactionType transactionType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    // MySQL owns this timestamp; Hibernate reads the generated value back.
    @Generated(event = EventType.INSERT)
    @Column(
        name = "created_at",
        columnDefinition = "datetime",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private LocalDateTime createdAt;
}
