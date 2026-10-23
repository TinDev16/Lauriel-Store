package com.lauriel.store.entity;

import com.lauriel.store.entity.enums.PaymentMethod;
import com.lauriel.store.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_code", length = 150)
    private String transactionCode;

    @Column(name = "paid_at", columnDefinition = "datetime")
    private LocalDateTime paidAt;

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
