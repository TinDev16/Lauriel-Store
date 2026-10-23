package com.lauriel.store.repository;

import com.lauriel.store.entity.Promotion;
import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface PromotionRepository
    extends
        JpaRepository<Promotion, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Promotion>
{
    Page<Promotion> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        ActiveStatus status,
        LocalDateTime start,
        LocalDateTime end,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.promotionId = :promotionId")
    Optional<Promotion> findByIdForUpdate(Long promotionId);
}
