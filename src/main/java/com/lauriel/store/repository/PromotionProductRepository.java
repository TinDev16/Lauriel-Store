package com.lauriel.store.repository;

import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface PromotionProductRepository
    extends JpaRepository<PromotionProduct, PromotionProductId>
{
    @Query(
        "select pp from PromotionProduct pp join fetch pp.promotion p " +
            "where pp.id.productId in :productIds and p.status = :status " +
            "and p.startDate <= :now and p.endDate >= :now order by p.promotionId"
    )
    List<PromotionProduct> findActiveCandidates(
        Collection<Long> productIds,
        ActiveStatus status,
        LocalDateTime now
    );

    @EntityGraph(attributePaths = { "product" })
    Page<PromotionProduct> findById_PromotionId(
        Long promotionId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = { "product" })
    @Query(
        value = "select pp from PromotionProduct pp where pp.id.promotionId = :promotionId " +
            "and (:keyword is null or lower(pp.product.productName) like lower(concat('%', :keyword, '%'))) " +
            "and (:status is null or pp.product.status = :status)",
        countQuery = "select count(pp) from PromotionProduct pp where pp.id.promotionId = :promotionId " +
            "and (:keyword is null or lower(pp.product.productName) like lower(concat('%', :keyword, '%'))) " +
            "and (:status is null or pp.product.status = :status)"
    )
    Page<PromotionProduct> findAdminProducts(
        Long promotionId,
        String keyword,
        ActiveStatus status,
        Pageable pageable
    );

    @Query(
        "select pp.id.productId from PromotionProduct pp where pp.id.promotionId = :promotionId " +
            "and pp.id.productId in :productIds"
    )
    List<Long> findLinkedProductIds(
        Long promotionId,
        Collection<Long> productIds
    );
}
