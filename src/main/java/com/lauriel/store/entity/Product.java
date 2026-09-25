package com.lauriel.store.entity;

import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "product_name", length = 200, nullable = false)
    private String productName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "img_url", columnDefinition = "text")
    private String imgUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", columnDefinition = "datetime", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", columnDefinition = "datetime", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, productImageId ASC")
    private List<ProductImage> images = new ArrayList<>();
}
