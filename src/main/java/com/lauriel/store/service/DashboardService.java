package com.lauriel.store.service;
import com.lauriel.store.repository.*;
import com.lauriel.store.entity.enums.*;
import java.time.Clock;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class DashboardService {
    public record Summary(long activeProducts, long pendingOrders, long todayOrders,
                          long customers, long draftReceipts, long lowStock,
                          long deliveredOrders, BigDecimal deliveredRevenue) {}
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final PurchaseReceiptRepository receipts;
    private final Clock clock;
    public DashboardService(ProductRepository products, ProductVariantRepository variants, OrderRepository orders,
            CustomerRepository customers, PurchaseReceiptRepository receipts, Clock clock) {
        this.products=products; this.variants=variants; this.orders=orders; this.customers=customers;
        this.receipts=receipts; this.clock=clock;
    }
    @Transactional(readOnly=true)
    public Summary summary() {
        var today = LocalDate.now(clock).atStartOfDay();
        return new Summary(products.countByStatus(ActiveStatus.ACTIVE),
                orders.countByStatus(OrderStatus.PENDING), orders.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(today, today.plusDays(1)),
                customers.count(), receipts.countByStatus(PurchaseReceiptStatus.DRAFT),
                variants.countByQuantityLessThanEqual(5),
                orders.countByStatus(OrderStatus.DELIVERED),
                orders.sumFinalAmountByStatus(OrderStatus.DELIVERED));
    }
}
