package com.lauriel.store.repository;

import com.lauriel.store.entity.InventoryTransaction;
import com.lauriel.store.entity.enums.InventoryTransactionType;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;

public interface InventoryTransactionRepository
    extends
        JpaRepository<InventoryTransaction, Long>,
        JpaSpecificationExecutor<InventoryTransaction>
{
    List<InventoryTransaction> findByReferenceTypeAndReferenceIdAndTransactionType(
        String referenceType,
        Long referenceId,
        InventoryTransactionType type
    );

    @Override
    @EntityGraph(attributePaths = { "productVariant.product", "user" })
    Page<InventoryTransaction> findAll(
        Specification<InventoryTransaction> specification,
        Pageable pageable
    );
}
