package com.lauriel.store.repository;

import com.lauriel.store.entity.Product;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :id")
    Optional<Product> findByIdForUpdate(Long id);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where p.productId <> :currentId
              and p.status = :status
              and p.category.status = :status
              and p.category.categoryId = :categoryId
              and exists (select v.variantId from ProductVariant v
                          where v.product = p and v.status = :status)
            order by p.createdAt desc, p.productId desc
            """)
    List<Product> findRelated(Long currentId, Long categoryId, ActiveStatus status, Pageable pageable);

    long countByStatus(ActiveStatus status);
    long countByCategory_CategoryId(Long categoryId);
    @EntityGraph(attributePaths = "category")
    Optional<Product> findByProductIdAndStatusAndCategory_Status(Long productId, ActiveStatus productStatus,
                                                                 ActiveStatus categoryStatus);
    boolean existsByProductIdAndStatus(Long productId, ActiveStatus status);
}
