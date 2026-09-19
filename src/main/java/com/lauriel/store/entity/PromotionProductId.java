package com.lauriel.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PromotionProductId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

}
