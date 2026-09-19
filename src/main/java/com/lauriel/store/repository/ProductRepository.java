package com.lauriel.store.repository;

import com.lauriel.store.entity.Product;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    long countByStatus(ActiveStatus status);
    Optional<Product> findByProductIdAndStatus(Long productId, ActiveStatus status);
    boolean existsByProductIdAndStatus(Long productId, ActiveStatus status);
}
