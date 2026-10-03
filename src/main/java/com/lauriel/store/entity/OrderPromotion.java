package com.lauriel.store.entity;

import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_promotions")
@Getter
@Setter
public class OrderPromotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_promotion_id")
    private Long orderPromotionId;

    @Column(name = "promotion_name", length = 200, nullable = false)
    private String promotionName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "min_order_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal minOrderAmount;

    @Column(name = "discount_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal discountPercent;

    @Column(name = "max_discount_amount", precision = 14, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "start_date", columnDefinition = "datetime", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", columnDefinition = "datetime", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;
}
