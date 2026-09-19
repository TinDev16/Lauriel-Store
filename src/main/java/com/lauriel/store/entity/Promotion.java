package com.lauriel.store.entity;

import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(name = "promotion_name", length = 200, nullable = false)
    private String promotionName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "discount_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal discountPercent;

    @Column(name = "start_date", columnDefinition = "datetime", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", columnDefinition = "datetime", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    @OneToMany(mappedBy = "promotion", fetch = FetchType.LAZY)
    private List<PromotionProduct> promotionProducts = new ArrayList<>();

}
