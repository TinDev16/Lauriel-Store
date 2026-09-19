package com.lauriel.store.entity;

import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
// Metadata edits must not rewrite stock held by a previously loaded entity.
@DynamicUpdate
@Table(name = "product_variants", uniqueConstraints = @UniqueConstraint(
        name = "uq_product_size_color", columnNames = {"product_id", "size_id", "color_id"}))
@Getter
@Setter
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "size_id", nullable = false)
    private Size size;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "color_id", nullable = false)
    private Color color;

    @Column(name = "sku", length = 100, nullable = false, unique = true)
    private String sku;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

}
