package com.lauriel.store.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_details")
@Getter
@Setter
public class OrderDetail {
    @EmbeddedId
    private OrderDetailId id = new OrderDetailId();

    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 14, scale = 2, nullable = false)
    private BigDecimal subtotal;

}
