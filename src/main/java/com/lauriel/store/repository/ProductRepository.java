package com.lauriel.store.repository;

import com.lauriel.store.entity.Product;
import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>
{
    @Override
    @EntityGraph(attributePaths = "category")
    List<Product> findAll(
        org.springframework.data.jpa.domain.Specification<Product> spec,
        org.springframework.data.domain.Sort sort
    );

    @Query(
        value = """
        SELECT p.*
        FROM products p
        JOIN categories c ON c.category_id = p.category_id
        LEFT JOIN (
            SELECT v.product_id, SUM(d.quantity) AS sold_quantity,
                   SUM(d.subtotal) AS sold_revenue
            FROM order_details d
            JOIN orders o ON o.order_id = d.order_id
            JOIN product_variants v ON v.variant_id = d.variant_id
            WHERE o.status = 'DELIVERED'
            GROUP BY v.product_id
        ) sales ON sales.product_id = p.product_id
        WHERE p.status = 'ACTIVE' AND c.status = 'ACTIVE'
        ORDER BY COALESCE(sales.sold_quantity, 0) DESC,
                 COALESCE(sales.sold_revenue, 0) DESC,
                 p.created_at DESC, p.product_id DESC
        """,
        nativeQuery = true
    )
    List<Product> findFeaturedProducts(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :id")
    Optional<Product> findByIdForUpdate(Long id);

    @EntityGraph(attributePaths = "category")
    @Query(
        """
        select p from Product p
        where p.productId <> :currentId
          and p.status = :status
          and p.category.status = :status
          and p.category.categoryId = :categoryId
          and exists (select v.variantId from ProductVariant v
                      where v.product = p and v.status = :status)
        order by p.createdAt desc, p.productId desc
        """
    )
    List<Product> findRelated(
        Long currentId,
        Long categoryId,
        ActiveStatus status,
        Pageable pageable
    );

    long countByStatus(ActiveStatus status);
    long countByCategory_CategoryId(Long categoryId);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findByProductIdAndStatusAndCategory_Status(
        Long productId,
        ActiveStatus productStatus,
        ActiveStatus categoryStatus
    );

    boolean existsByProductIdAndStatus(Long productId, ActiveStatus status);

    @EntityGraph(attributePaths = "category")
    @Query(
        """
        select p from Product p
        where (:keyword is null or lower(p.productName) like lower(concat('%', :keyword, '%')))
          and (:categoryId is null or p.category.categoryId = :categoryId)
          and not exists (
              select pp.id.productId from PromotionProduct pp
              where pp.id.promotionId = :promotionId and pp.id.productId = p.productId
          )
        order by p.category.categoryName, p.productName, p.productId
        """
    )
    List<Product> findPromotionCandidates(
        Long promotionId,
        String keyword,
        Long categoryId,
        Pageable pageable
    );
}
