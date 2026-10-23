package com.lauriel.store.repository;

import com.lauriel.store.entity.OrderDetail;
import com.lauriel.store.entity.OrderDetailId;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface OrderDetailRepository
    extends JpaRepository<OrderDetail, OrderDetailId>
{
    @EntityGraph(
        attributePaths = {
            "productVariant.product",
            "productVariant.size",
            "productVariant.color",
        }
    )
    List<OrderDetail> findById_OrderIdOrderById_VariantIdAsc(Long orderId);

    // Restocking must not load quantity from variants before their write locks are acquired.
    @Query(
        "select d from OrderDetail d where d.id.orderId = :orderId order by d.id.variantId"
    )
    List<OrderDetail> findItemsForCancellation(Long orderId);
}
