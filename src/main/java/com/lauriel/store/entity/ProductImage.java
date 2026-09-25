package com.lauriel.store.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "product_images")
@Getter
@Setter
public class ProductImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_image_id")
    private Long productImageId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(name = "image_url", columnDefinition = "text", nullable = false)
    private String imageUrl;
    @Column(name = "is_primary", nullable = false)
    private boolean primary;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", columnDefinition = "datetime", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
