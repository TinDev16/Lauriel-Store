package com.lauriel.store.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PromotionOverviewResponse(
    // Tổng hợp chung
    int totalActiveCampaigns,
    int totalUpcomingCampaigns,
    int totalExpiredCampaigns,
    int totalCampaigns,

    // Khuyến mãi sản phẩm
    int activeProductPromotionsCount,
    int upcomingProductPromotionsCount,
    int expiredProductPromotionsCount,
    int inactiveProductPromotionsCount,
    int totalProductPromotionsCount,

    // Mã giảm giá (Vouchers)
    int activeVouchersCount,
    int outOfStockVouchersCount,
    int upcomingVouchersCount,
    int expiredVouchersCount,
    int inactiveVouchersCount,
    int totalVouchersCount,

    // Khuyến mãi đơn hàng
    int activeOrderPromotionsCount,
    int upcomingOrderPromotionsCount,
    int expiredOrderPromotionsCount,
    int inactiveOrderPromotionsCount,
    int totalOrderPromotionsCount,

    // Danh sách
    List<ActiveProductPromotionItem> activeProductPromotions,
    List<VoucherResponse> activeVouchers,
    List<OrderPromotionResponse> activeOrderPromotions,
    List<UpcomingCampaignItem> upcomingCampaigns
) {

    public record ActiveProductPromotionItem(
        Long promotionId,
        String promotionName,
        String description,
        BigDecimal discountPercent,
        LocalDateTime startDate,
        LocalDateTime endDate,
        long appliedProductCount
    ) {}

    public record UpcomingCampaignItem(
        String type,
        String typeLabel,
        String name,
        String discountDetail,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String url
    ) {}
}
