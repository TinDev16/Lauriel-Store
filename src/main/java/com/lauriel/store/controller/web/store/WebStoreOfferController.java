package com.lauriel.store.controller.web.store;

import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.entity.enums.DiscountType;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.service.OrderPromotionService;
import com.lauriel.store.service.PricingService;
import com.lauriel.store.service.PromotionService;
import com.lauriel.store.service.VoucherService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shop/offers")
public class WebStoreOfferController {
    private final PromotionService promotions;
    private final OrderPromotionService orderPromotions;
    private final VoucherService vouchers;
    private final PricingService pricing;

    public WebStoreOfferController(PromotionService promotions, OrderPromotionService orderPromotions,
            VoucherService vouchers, PricingService pricing) {
        this.promotions = promotions;
        this.orderPromotions = orderPromotions;
        this.vouchers = vouchers;
        this.pricing = pricing;
    }

    @GetMapping("/products/{id}")
    public String productPromotion(@PathVariable Long id, Model model) {
        var offer = promotions.detail(id);
        requireCurrent(offer.status(), offer.startDate(), offer.endDate());
        common(model, "PRODUCT", "GIẢM GIÁ SẢN PHẨM", offer.promotionName(), offer.description(),
                offer.discountPercent(), null, null, null, null, null, offer.startDate(), offer.endDate());
        model.addAttribute("products",
                promotions.products(id, 0, 24, null, ActiveStatus.ACTIVE).content());
        return "store/offer-detail";
    }

    @GetMapping("/orders/{id}")
    public String orderPromotion(@PathVariable Long id, Model model) {
        var offer = orderPromotions.detail(id);
        requireCurrent(offer.status(), offer.startDate(), offer.endDate());
        common(model, "ORDER", "ƯU ĐÃI HÓA ĐƠN", offer.promotionName(), offer.description(),
                offer.discountPercent(), null, offer.minOrderAmount(), offer.maxDiscountAmount(), null, null,
                offer.startDate(), offer.endDate());
        return "store/offer-detail";
    }

    @GetMapping("/vouchers/{id}")
    public String voucher(@PathVariable Long id, Model model) {
        var offer = vouchers.detail(id);
        requireCurrent(offer.status(), offer.startDate(), offer.endDate());
        BigDecimal percent = offer.discountType() == DiscountType.PERCENT ? offer.discountValue() : null;
        BigDecimal fixed = offer.discountType() == DiscountType.FIXED ? offer.discountValue() : null;
        common(model, "VOUCHER", "MÃ GIẢM GIÁ", offer.voucherName(), null, percent, fixed,
                offer.minOrderAmount(), offer.maxDiscountAmount(), offer.code(), offer.quantity(),
                offer.startDate(), offer.endDate());
        return "store/offer-detail";
    }

    private void common(Model model, String type, String label, String title, String description,
            BigDecimal discountPercent, BigDecimal discountValue, BigDecimal minOrderAmount,
            BigDecimal maxDiscountAmount, String code, Integer quantity,
            LocalDateTime startDate, LocalDateTime endDate) {
        model.addAttribute("offerType", type);
        model.addAttribute("offerLabel", label);
        model.addAttribute("offerTitle", title);
        model.addAttribute("offerDescription", description);
        model.addAttribute("discountPercent", discountPercent);
        model.addAttribute("discountValue", discountValue);
        model.addAttribute("minOrderAmount", minOrderAmount);
        model.addAttribute("maxDiscountAmount", maxDiscountAmount);
        model.addAttribute("voucherCode", code);
        model.addAttribute("remainingQuantity", quantity);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("products", List.of());
    }

    private void requireCurrent(ActiveStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        var now = pricing.now();
        if (status != ActiveStatus.ACTIVE || startDate == null || endDate == null
                || startDate.isAfter(now) || endDate.isBefore(now)) {
            throw new ResourceNotFoundException("Ưu đãi không còn hiệu lực");
        }
    }
}
