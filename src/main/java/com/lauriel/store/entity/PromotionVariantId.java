package com.lauriel.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class PromotionVariantId implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column(name = "promotion_id", nullable = false) private Long promotionId;
    @Column(name = "variant_id", nullable = false) private Long variantId;
}
