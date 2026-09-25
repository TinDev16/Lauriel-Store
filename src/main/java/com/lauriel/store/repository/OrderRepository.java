package com.lauriel.store.repository;

import com.lauriel.store.entity.Order;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    long countByStatus(com.lauriel.store.entity.enums.OrderStatus status);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(java.time.LocalDateTime from, java.time.LocalDateTime to);
    @Override
    @EntityGraph(attributePaths = {"customer", "voucher"})
    Optional<Order> findById(Long orderId);

    @EntityGraph(attributePaths = {"customer", "voucher"})
    Optional<Order> findByOrderIdAndCustomer_CustomerId(Long orderId, Long customerId);

    @EntityGraph(attributePaths = {"customer"})
    Page<Order> findByCustomer_CustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<Order> findByCustomer_CustomerIdAndStatus(Long customerId,
            com.lauriel.store.entity.enums.OrderStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"customer"})
    Page<Order> findAll(Specification<Order> filter, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :orderId")
    Optional<Order> findByIdForUpdate(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :orderId and o.customer.customerId = :customerId")
    Optional<Order> findOwnedByIdForUpdate(Long orderId, Long customerId);
}
