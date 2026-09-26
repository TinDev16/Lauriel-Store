package com.lauriel.store.repository;

import com.lauriel.store.entity.ProductImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    @EntityGraph(attributePaths = "color")
    List<ProductImage> findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(Long productId);
    Optional<ProductImage> findByProductImageIdAndProduct_ProductId(Long imageId, Long productId);
    long countByProduct_ProductId(Long productId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductImage i set i.primary = false where i.product.productId = :productId and i.primary = true")
    void clearPrimary(@Param("productId") Long productId);
}
