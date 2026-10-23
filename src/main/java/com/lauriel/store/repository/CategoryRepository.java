package com.lauriel.store.repository;

import com.lauriel.store.dto.response.CategoryResponse;
import com.lauriel.store.entity.Category;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCategoryNameIgnoreCase(String categoryName);
    boolean existsByCategoryNameIgnoreCase(String categoryName);
    boolean existsByCategoryNameIgnoreCaseAndCategoryIdNot(
        String categoryName,
        Long categoryId
    );
    List<Category> findByStatusOrderByCategoryNameAscCategoryIdAsc(
        ActiveStatus status
    );

    @Query(
        """
        select new com.lauriel.store.dto.response.CategoryResponse(
            c.categoryId, c.categoryName, c.status, c.createdAt, count(p.productId))
        from Category c left join c.products p
        group by c.categoryId, c.categoryName, c.status, c.createdAt
        order by c.status, c.categoryName, c.categoryId
        """
    )
    List<CategoryResponse> findAllWithProductCount();
}
