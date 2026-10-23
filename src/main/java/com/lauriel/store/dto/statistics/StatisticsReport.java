package com.lauriel.store.dto.statistics;

import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.entity.enums.SalesChannel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatisticsReport(
    Range range,
    Kpis kpis,
    Sales sales,
    List<RevenuePoint> revenue,
    List<StatusPoint> statuses,
    List<TopProduct> topProducts,
    List<TopProduct> topProductsByRevenue,
    List<TopVariant> topVariants,
    List<TopCustomer> topCustomers,
    List<TopCustomer> topCustomersByOrders,
    Inventory inventory,
    List<StockVariant> lowStock,
    List<StockVariant> topStock,
    Purchase purchases,
    List<PurchasePoint> purchaseTrend,
    List<Supplier> topSuppliers,
    Discounts discounts,
    List<Voucher> topVouchersByUses,
    List<Voucher> topVouchersByDiscount,
    List<PromotionProduct> topPromotionProducts,
    List<OrderPromotion> topOrderPromotionsByUses,
    List<OrderPromotion> topOrderPromotionsByDiscount,
    long soldQuantity,
    long customersWithOrders,
    long newCustomersThisMonth,
    List<CustomerPoint> newCustomers,
    List<ChannelPerformance> channelPerformance,
    Engagement engagement,
    List<OnlineCustomer> onlineCustomers,
    List<CartInterest> cartInterest
) {
    public record Range(
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        String groupBy,
        int threshold,
        int top
    ) {}

    public record Kpis(
        BigDecimal revenueAllTime,
        BigDecimal revenueToday,
        BigDecimal revenueMonth,
        BigDecimal revenueYear,
        long ordersToday,
        long ordersMonth,
        long deliveredAllTime,
        long cancelledAllTime,
        long customers,
        long activeProducts,
        long activeVariants,
        long totalStock,
        long lowStock,
        long draftReceipts,
        BigDecimal purchaseMonth
    ) {}

    public record Sales(
        long totalOrders,
        long delivered,
        long cancelled,
        BigDecimal revenue,
        BigDecimal averageOrderValue,
        BigDecimal cancelRate
    ) {}

    public record RevenuePoint(
        String label,
        BigDecimal revenue,
        long orderCount
    ) {}

    public record StatusPoint(
        OrderStatus status,
        long count,
        BigDecimal percentage
    ) {}

    public record TopProduct(
        long productId,
        String productName,
        long quantity,
        BigDecimal revenue
    ) {}

    public record TopVariant(
        long variantId,
        String sku,
        String productName,
        String size,
        String color,
        long quantity,
        BigDecimal revenue
    ) {}

    public record TopCustomer(
        long customerId,
        String customerName,
        long orderCount,
        BigDecimal totalSpent
    ) {}

    public record Inventory(
        long totalQuantity,
        long activeVariants,
        long lowStock,
        long outOfStock,
        BigDecimal valueBySellingPrice
    ) {}

    public record StockVariant(
        long variantId,
        String sku,
        String productName,
        String size,
        String color,
        long quantity
    ) {}

    public record Purchase(long receipts, BigDecimal amount, long quantity) {}

    public record PurchasePoint(
        String label,
        long receipts,
        BigDecimal amount,
        long quantity
    ) {}

    public record Supplier(
        long supplierId,
        String supplierName,
        long receiptCount,
        BigDecimal purchaseAmount
    ) {}

    public record Discounts(
        BigDecimal voucher,
        BigDecimal productPromotion,
        BigDecimal orderPromotion,
        BigDecimal total,
        long voucherOrders,
        long promotedQuantity,
        long orderPromotionOrders
    ) {}

    public record Voucher(
        long voucherId,
        String code,
        long orderCount,
        BigDecimal discount
    ) {}

    public record PromotionProduct(
        long productId,
        String productName,
        long quantity,
        BigDecimal discount
    ) {}

    public record OrderPromotion(
        Long promotionId,
        String promotionName,
        long orderCount,
        BigDecimal discount
    ) {}

    public record CustomerPoint(String label, long count) {}

    public record ChannelPerformance(
        SalesChannel channel,
        long buyers,
        long orders,
        long delivered,
        long cancelled,
        BigDecimal revenue,
        BigDecimal averageOrderValue,
        BigDecimal buyerShare,
        BigDecimal orderShare
    ) {}

    public record Engagement(
        long onlineCustomers,
        long customersWithCart,
        long distinctProductsInCarts,
        long cartQuantity
    ) {}

    public record OnlineCustomer(
        long customerId,
        String customerName,
        String username,
        java.time.LocalDateTime lastSeenAt,
        long distinctItems,
        long quantity
    ) {}

    public record CartInterest(
        long productId,
        String productName,
        long customerCount,
        long quantity
    ) {}
}
