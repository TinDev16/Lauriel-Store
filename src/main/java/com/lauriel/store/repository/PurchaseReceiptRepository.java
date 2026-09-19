package com.lauriel.store.repository;

import com.lauriel.store.entity.PurchaseReceipt;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long> {
    long countByStatus(com.lauriel.store.entity.enums.PurchaseReceiptStatus status);
    @Override
    @EntityGraph(attributePaths = {"supplier", "user"})
    Page<PurchaseReceipt> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"supplier", "user"})
    Optional<PurchaseReceipt> findById(Long receiptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PurchaseReceipt r where r.receiptId = :receiptId")
    Optional<PurchaseReceipt> findByIdForUpdate(Long receiptId);
}
