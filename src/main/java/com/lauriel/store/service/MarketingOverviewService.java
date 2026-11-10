package com.lauriel.store.service;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.OrderPromotion;
import com.lauriel.store.entity.Promotion;
import com.lauriel.store.entity.Voucher;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.entity.enums.DiscountType;
import com.lauriel.store.mapper.MarketingMapper;
import com.lauriel.store.repository.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingOverviewService {

    private final PromotionRepository promotions;
    private final VoucherRepository vouchers;
    private final OrderPromotionRepository orderPromotions;
    private final PromotionProductRepository promotionProducts;
    private final PricingService pricing;

    public MarketingOverviewService(
        PromotionRepository promotions,
        VoucherRepository vouchers,
        OrderPromotionRepository orderPromotions,
        PromotionProductRepository promotionProducts,
        PricingService pricing
    ) {
        this.promotions = promotions;
        this.vouchers = vouchers;
        this.orderPromotions = orderPromotions;
        this.promotionProducts = promotionProducts;
        this.pricing = pricing;
    }

    @Transactional(readOnly = true)
    public PromotionOverviewResponse getOverview() {
        LocalDateTime now = pricing.now();

        List<Promotion> allPromotions = promotions.findAll();
        List<Voucher> allVouchers = vouchers.findAll();
        List<OrderPromotion> allOrderPromotions = orderPromotions.findAll();

        // 1. Khuyến mãi sản phẩm
        List<PromotionOverviewResponse.ActiveProductPromotionItem> activeProductPromotions = new ArrayList<>();
        int upcomingProductPromotionsCount = 0;
        int expiredProductPromotionsCount = 0;
        int inactiveProductPromotionsCount = 0;

        for (Promotion p : allPromotions) {
            boolean isRunning = p.getStatus() == ActiveStatus.ACTIVE &&
                !now.isBefore(p.getStartDate()) &&
                !now.isAfter(p.getEndDate());
            if (isRunning) {
                long productCount = promotionProducts.countById_PromotionId(p.getPromotionId());
                activeProductPromotions.add(new PromotionOverviewResponse.ActiveProductPromotionItem(
                    p.getPromotionId(),
                    p.getPromotionName(),
                    p.getDescription(),
                    p.getDiscountPercent(),
                    p.getStartDate(),
                    p.getEndDate(),
                    productCount
                ));
            } else if (p.getStatus() == ActiveStatus.ACTIVE && p.getStartDate().isAfter(now)) {
                upcomingProductPromotionsCount++;
            } else if (p.getStatus() == ActiveStatus.INACTIVE) {
                inactiveProductPromotionsCount++;
            }
            if (p.getEndDate().isBefore(now)) {
                expiredProductPromotionsCount++;
            }
        }
        activeProductPromotions.sort(Comparator.comparing(PromotionOverviewResponse.ActiveProductPromotionItem::endDate));

        // 2. Mã giảm giá (Vouchers)
        List<VoucherResponse> activeVouchers = new ArrayList<>();
        int upcomingVouchersCount = 0;
        int expiredVouchersCount = 0;
        int outOfStockVouchersCount = 0;
        int inactiveVouchersCount = 0;

        for (Voucher v : allVouchers) {
            boolean isAvailable = v.getStatus() == ActiveStatus.ACTIVE &&
                v.getQuantity() > 0 &&
                !now.isBefore(v.getStartDate()) &&
                !now.isAfter(v.getEndDate());
            if (isAvailable) {
                activeVouchers.add(MarketingMapper.voucher(v));
            } else if (v.getStatus() == ActiveStatus.ACTIVE && v.getStartDate().isAfter(now)) {
                upcomingVouchersCount++;
            } else if (v.getStatus() == ActiveStatus.INACTIVE) {
                inactiveVouchersCount++;
            }
            if (v.getQuantity() <= 0) {
                outOfStockVouchersCount++;
            }
            if (v.getEndDate().isBefore(now)) {
                expiredVouchersCount++;
            }
        }
        activeVouchers.sort(Comparator.comparing(VoucherResponse::endDate));

        // 3. Khuyến mãi đơn hàng
        List<OrderPromotionResponse> activeOrderPromotions = new ArrayList<>();
        int upcomingOrderPromotionsCount = 0;
        int expiredOrderPromotionsCount = 0;
        int inactiveOrderPromotionsCount = 0;

        for (OrderPromotion op : allOrderPromotions) {
            boolean isRunning = op.getStatus() == ActiveStatus.ACTIVE &&
                !now.isBefore(op.getStartDate()) &&
                !now.isAfter(op.getEndDate());
            if (isRunning) {
                activeOrderPromotions.add(MarketingMapper.orderPromotion(op));
            } else if (op.getStatus() == ActiveStatus.ACTIVE && op.getStartDate().isAfter(now)) {
                upcomingOrderPromotionsCount++;
            } else if (op.getStatus() == ActiveStatus.INACTIVE) {
                inactiveOrderPromotionsCount++;
            }
            if (op.getEndDate().isBefore(now)) {
                expiredOrderPromotionsCount++;
            }
        }
        activeOrderPromotions.sort(Comparator.comparing(OrderPromotionResponse::endDate));

        // 4. Danh sách sắp diễn ra (Upcoming)
        List<PromotionOverviewResponse.UpcomingCampaignItem> upcomingCampaigns = new ArrayList<>();
        for (Promotion p : allPromotions) {
            if (p.getStatus() == ActiveStatus.ACTIVE && p.getStartDate().isAfter(now)) {
                upcomingCampaigns.add(new PromotionOverviewResponse.UpcomingCampaignItem(
                    "PRODUCT",
                    "Khuyến mãi sản phẩm",
                    p.getPromotionName(),
                    "Giảm " + p.getDiscountPercent() + "%",
                    p.getStartDate(),
                    p.getEndDate(),
                    "/admin/promotions/" + p.getPromotionId() + "/edit"
                ));
            }
        }
        for (Voucher v : allVouchers) {
            if (v.getStatus() == ActiveStatus.ACTIVE && v.getStartDate().isAfter(now)) {
                String detail = v.getDiscountType() == DiscountType.PERCENT
                    ? "Giảm " + v.getDiscountValue() + "%"
                    : "Giảm " + v.getDiscountValue() + "₫";
                upcomingCampaigns.add(new PromotionOverviewResponse.UpcomingCampaignItem(
                    "VOUCHER",
                    "Mã giảm giá",
                    v.getCode() + " - " + v.getVoucherName(),
                    detail,
                    v.getStartDate(),
                    v.getEndDate(),
                    "/admin/vouchers/" + v.getVoucherId() + "/edit"
                ));
            }
        }
        for (OrderPromotion op : allOrderPromotions) {
            if (op.getStatus() == ActiveStatus.ACTIVE && op.getStartDate().isAfter(now)) {
                upcomingCampaigns.add(new PromotionOverviewResponse.UpcomingCampaignItem(
                    "ORDER",
                    "Khuyến mãi đơn hàng",
                    op.getPromotionName(),
                    "Giảm " + op.getDiscountPercent() + "% cho đơn từ " + op.getMinOrderAmount() + "₫",
                    op.getStartDate(),
                    op.getEndDate(),
                    "/admin/order-promotions/" + op.getOrderPromotionId() + "/edit"
                ));
            }
        }
        upcomingCampaigns.sort(Comparator.comparing(PromotionOverviewResponse.UpcomingCampaignItem::startDate));

        int totalActive = activeProductPromotions.size() + activeVouchers.size() + activeOrderPromotions.size();
        int totalUpcoming = upcomingProductPromotionsCount + upcomingVouchersCount + upcomingOrderPromotionsCount;
        int totalExpired = expiredProductPromotionsCount + expiredVouchersCount + expiredOrderPromotionsCount;
        int totalAll = allPromotions.size() + allVouchers.size() + allOrderPromotions.size();

        return new PromotionOverviewResponse(
            totalActive,
            totalUpcoming,
            totalExpired,
            totalAll,
            activeProductPromotions.size(),
            upcomingProductPromotionsCount,
            expiredProductPromotionsCount,
            inactiveProductPromotionsCount,
            allPromotions.size(),
            activeVouchers.size(),
            outOfStockVouchersCount,
            upcomingVouchersCount,
            expiredVouchersCount,
            inactiveVouchersCount,
            allVouchers.size(),
            activeOrderPromotions.size(),
            upcomingOrderPromotionsCount,
            expiredOrderPromotionsCount,
            inactiveOrderPromotionsCount,
            allOrderPromotions.size(),
            activeProductPromotions,
            activeVouchers,
            activeOrderPromotions,
            upcomingCampaigns
        );
    }
}
