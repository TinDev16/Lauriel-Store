package com.lauriel.store.entity;

import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.entity.enums.DiscountType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Long voucherId;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "voucher_name", length = 200, nullable = false)
    private String voucherName;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(
        name = "discount_value",
        precision = 12,
        scale = 2,
        nullable = false
    )
    private BigDecimal discountValue;

    @Column(
        name = "min_order_amount",
        precision = 12,
        scale = 2,
        nullable = false
    )
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(
        name = "start_date",
        columnDefinition = "datetime",
        nullable = false
    )
    private LocalDateTime startDate;

    @Column(name = "end_date", columnDefinition = "datetime", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;
}
