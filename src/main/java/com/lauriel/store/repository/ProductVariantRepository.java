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
    interface ProductInventorySummary {
        Long getProductId();
        String getProductName();
        String getImageUrl();
        String getRepresentativeSku();
        long getVariantCount();
        long getTotalQuantity();
        long getOutOfStockCount();
        ActiveStatus getStatus();
    }

    @Query(value = """
            select v.product.productId as productId,
                   v.product.productName as productName,
                   v.product.imgUrl as imageUrl,
                   min(v.sku) as representativeSku,
                   count(v.variantId) as variantCount,
                   sum(v.quantity) as totalQuantity,
                   sum(case when v.quantity = 0 then 1 else 0 end) as outOfStockCount,
                   v.product.status as status
            from ProductVariant v
            where (:keyword is null or lower(v.product.productName) like lower(concat('%', :keyword, '%')))
            group by v.product.productId, v.product.productName, v.product.imgUrl, v.product.status
            having (:threshold is null or sum(v.quantity) <= :threshold)
            """,
            countQuery = """
            select count(distinct v.product.productId)
            from ProductVariant v
            where (:keyword is null or lower(v.product.productName) like lower(concat('%', :keyword, '%')))
              and (:threshold is null or v.product.productId in (
                    select v2.product.productId from ProductVariant v2
                    group by v2.product.productId having sum(v2.quantity) <= :threshold))
            """)
    Page<ProductInventorySummary> findInventoryProducts(String keyword, Integer threshold, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "size", "color"})
    List<ProductVariant> findByProduct_ProductIdOrderByColor_ColorNameAscSize_SizeNameAscVariantIdAsc(Long productId);

    long countByQuantityLessThanEqual(int quantity);
    @Query("select v.product.productId as productId, count(v) as variants, "
            + "coalesce(sum(v.quantity), 0) as totalQuantity from ProductVariant v "
            + "where v.product.productId in :ids group by v.product.productId")
    List<VariantCount> countForProducts(java.util.Collection<Long> ids);
    interface VariantCount {
        Long getProductId();
        long getVariants();
        long getTotalQuantity();
    }
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

    @Query("select v as variant, productPromotion as productPromotion, variantPromotion as variantPromotion "
            + "from ProductVariant v join fetch v.product join fetch v.size join fetch v.color "
            + "left join PromotionProduct pp on pp.product = v.product "
            + "left join pp.promotion productPromotion on productPromotion.status=:status "
            + "and productPromotion.startDate<=:now and productPromotion.endDate>=:now "
            + "and not exists (select scoped.id.variantId from PromotionVariant scoped "
            + "where scoped.promotion=productPromotion and scoped.variant.product=v.product) "
            + "left join PromotionVariant pv on pv.variant=v "
            + "left join pv.promotion variantPromotion on variantPromotion.status=:status "
            + "and variantPromotion.startDate<=:now and variantPromotion.endDate>=:now "
            + "where v.product.productId=:productId and v.status=:status "
            + "order by v.variantId, productPromotion.promotionId, variantPromotion.promotionId")
    List<VariantPromotionView> findPublicVariantsWithPromotions(Long productId, ActiveStatus status,
            java.time.LocalDateTime now);
    @Query("select v.size.sizeId as sizeId, v.color.colorId as colorId "
            + "from ProductVariant v where v.product.productId = :productId")
    List<VariantAttributes> findAttributesByProductId(Long productId);
    interface VariantAttributes {
        Long getSizeId();
        Long getColorId();
    }
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndVariantIdNot(String sku, Long variantId);
    boolean existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorId(Long productId, Long sizeId, Long colorId);
    boolean existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorIdAndVariantIdNot(
            Long productId, Long sizeId, Long colorId, Long variantId);
}
