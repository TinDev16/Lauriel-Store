package com.lauriel.store.repository;

import com.lauriel.store.entity.OrderPromotion;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;

public interface OrderPromotionRepository extends JpaRepository<OrderPromotion, Long>, JpaSpecificationExecutor<OrderPromotion> {
    @Query("select p from OrderPromotion p where p.status=:status and p.startDate<=:now and p.endDate>=:now and p.minOrderAmount<=:amount")
    List<OrderPromotion> findEligible(ActiveStatus status, LocalDateTime now, BigDecimal amount);

    @Query("select p from OrderPromotion p where p.status=:status and p.startDate<=:now and p.endDate>=:now")
    Page<OrderPromotion> findActive(ActiveStatus status, LocalDateTime now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from OrderPromotion p where p.orderPromotionId=:id")
    Optional<OrderPromotion> findByIdForUpdate(Long id);
}
