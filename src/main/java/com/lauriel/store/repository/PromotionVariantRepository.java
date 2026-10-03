package com.lauriel.store.repository;

import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PromotionVariantRepository extends JpaRepository<PromotionVariant, PromotionVariantId> {
    interface ScopeView { Long getPromotionId(); Long getProductId(); }

    @Query("select pv from PromotionVariant pv join fetch pv.promotion p "
            + "where pv.id.variantId in :variantIds and p.status=:status "
            + "and p.startDate<=:now and p.endDate>=:now order by p.promotionId")
    List<PromotionVariant> findActiveCandidates(Collection<Long> variantIds, ActiveStatus status, LocalDateTime now);

    @Query("select distinct pv.promotion.promotionId as promotionId, pv.variant.product.productId as productId "
            + "from PromotionVariant pv where pv.variant.product.productId in :productIds")
    List<ScopeView> findScopes(Collection<Long> productIds);

    @Query("select pv.id.variantId from PromotionVariant pv where pv.id.promotionId=:promotionId "
            + "and pv.variant.product.productId=:productId")
    List<Long> findSelectedVariantIds(Long promotionId, Long productId);

    @Modifying
    @Query("delete from PromotionVariant pv where pv.id.promotionId=:promotionId "
            + "and pv.id.variantId in :variantIds")
    int deleteSelected(Long promotionId, Collection<Long> variantIds);
}
