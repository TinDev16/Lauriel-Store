package com.lauriel.store.mapper;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.*;

public final class MarketingMapper {

    private MarketingMapper() {}

    public static VoucherResponse voucher(Voucher voucher) {
        return new VoucherResponse(
            voucher.getVoucherId(),
            voucher.getCode(),
            voucher.getVoucherName(),
            voucher.getDiscountType(),
            voucher.getDiscountValue(),
            voucher.getMinOrderAmount(),
            voucher.getMaxDiscountAmount(),
            voucher.getQuantity(),
            voucher.getStartDate(),
            voucher.getEndDate(),
            voucher.getStatus()
        );
    }

    public static PromotionResponse promotion(Promotion promotion) {
        return new PromotionResponse(
            promotion.getPromotionId(),
            promotion.getPromotionName(),
            promotion.getDescription(),
            promotion.getDiscountPercent(),
            promotion.getStartDate(),
            promotion.getEndDate(),
            promotion.getStatus()
        );
    }

    public static PromotionInfoResponse info(Promotion promotion) {
        return new PromotionInfoResponse(
            promotion.getPromotionId(),
            promotion.getPromotionName(),
            promotion.getDiscountPercent(),
            promotion.getStartDate(),
            promotion.getEndDate()
        );
    }

    public static OrderPromotionResponse orderPromotion(OrderPromotion p) {
        return new OrderPromotionResponse(
            p.getOrderPromotionId(),
            p.getPromotionName(),
            p.getDescription(),
            p.getMinOrderAmount(),
            p.getDiscountPercent(),
            p.getMaxDiscountAmount(),
            p.getStartDate(),
            p.getEndDate(),
            p.getStatus()
        );
    }
}
