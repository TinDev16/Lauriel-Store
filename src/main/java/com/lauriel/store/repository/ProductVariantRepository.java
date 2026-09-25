package com.lauriel.store.repository;

import com.lauriel.store.entity.ProductVariant;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {
    long countByQuantityLessThanEqual(int quantity);
    @Query("select v.product.productId as productId, count(v) as variants from ProductVariant v where v.product.productId in :ids group by v.product.productId")
    List<VariantCount> countForProducts(java.util.Collection<Long> ids);
    interface VariantCount { Long getProductId(); long getVariants(); }
    @EntityGraph(attributePaths = {"product", "size", "color"})
    org.springframework.data.domain.Page<ProductVariant> findByProduct_ProductId(Long productId, org.springframework.data.domain.Pageable pageable);
    @EntityGraph(attributePaths = {"product", "size", "color"})
    List<ProductVariant> findByProduct_ProductIdAndStatusOrderByVariantIdAsc(Long productId, ActiveStatus status);

    @Override
    @EntityGraph(attributePaths = {"product", "size", "color"})
    Optional<ProductVariant> findById(Long variantId);

    @Override
    @EntityGraph(attributePaths = {"product", "size", "color"})
    Page<ProductVariant> findAll(Specification<ProductVariant> specification, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.variantId = :variantId")
    Optional<ProductVariant> findByIdForUpdate(Long variantId);
    @EntityGraph(attributePaths = {"product", "size", "color"})
    List<ProductVariant> findByVariantIdIn(java.util.Collection<Long> variantIds);

    @Query("select v from ProductVariant v join fetch v.product join fetch v.size join fetch v.color "
            + "order by v.product.productName, v.size.sizeName, v.color.colorName, v.variantId")
    List<ProductVariant> findAllForSelection();

    @Query("select v as variant, p as promotion from ProductVariant v "
            + "join fetch v.product join fetch v.size join fetch v.color "
            + "left join PromotionProduct pp on pp.product = v.product "
            + "left join pp.promotion p on p.status = :status and p.startDate <= :now and p.endDate >= :now "
            + "where v.product.productId = :productId and v.status = :status order by v.variantId, p.promotionId")
    List<VariantPromotionView> findPublicVariantsWithPromotions(Long productId, ActiveStatus status,
            java.time.LocalDateTime now);
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndVariantIdNot(String sku, Long variantId);
    boolean existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorId(Long productId, Long sizeId, Long colorId);
    boolean existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorIdAndVariantIdNot(
            Long productId, Long sizeId, Long colorId, Long variantId);
}
