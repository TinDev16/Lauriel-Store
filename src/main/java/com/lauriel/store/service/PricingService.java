package com.lauriel.store.service;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.*;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.mapper.*;
import com.lauriel.store.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");
    private final PromotionProductRepository associations;
    private final ProductVariantRepository variants;
    private final Clock clock;

    public PricingService(PromotionProductRepository associations, ProductVariantRepository variants, Clock clock) {
        this.associations = associations;
        this.variants = variants;
        this.clock = clock;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @Transactional(readOnly = true)
    public Map<Long, PriceCalculation> priceVariants(Collection<ProductVariant> variants, LocalDateTime now) {
        Map<Long, List<Promotion>> byProduct = new HashMap<>();
        var productIds = variants.stream().map(v -> v.getProduct().getProductId()).distinct().toList();
        if (!productIds.isEmpty()) {
            for (var association : associations.findActiveCandidates(productIds, ActiveStatus.ACTIVE, now)) {
                byProduct.computeIfAbsent(association.getId().getProductId(), ignored -> new ArrayList<>())
                        .add(association.getPromotion());
            }
        }
        Map<Long, PriceCalculation> prices = new LinkedHashMap<>();
        for (var variant : variants) {
            prices.put(variant.getVariantId(), bestPrice(variant.getPrice(),
                    byProduct.getOrDefault(variant.getProduct().getProductId(), List.of()), now));
        }
        return prices;
    }

    // One joined query fetches all public variants and promotion candidates, keeping detail query count constant.
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> publicVariants(Long productId) {
        var now = now();
        Map<Long, ProductVariant> found = new LinkedHashMap<>();
        Map<Long, List<Promotion>> candidates = new HashMap<>();
        for (var row : variants.findPublicVariantsWithPromotions(productId, ActiveStatus.ACTIVE, now)) {
            var variant = row.getVariant();
            found.putIfAbsent(variant.getVariantId(), variant);
            if (row.getPromotion() != null) {
                candidates.computeIfAbsent(variant.getVariantId(), ignored -> new ArrayList<>()).add(row.getPromotion());
            }
        }
        return found.values().stream().map(variant -> response(variant,
                bestPrice(variant.getPrice(), candidates.getOrDefault(variant.getVariantId(), List.of()), now))).toList();
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse variantResponse(ProductVariant variant) {
        return response(variant, priceVariants(List.of(variant), now()).get(variant.getVariantId()));
    }

    public PriceCalculation bestPrice(BigDecimal base, Collection<Promotion> promotions, LocalDateTime now) {
        BigDecimal bestDiscount = BigDecimal.ZERO.setScale(2);
        Promotion best = null;
        for (var promotion : promotions) {
            if (!active(promotion.getStatus(), promotion.getStartDate(), promotion.getEndDate(), now)
                    || promotion.getDiscountPercent().signum() <= 0
                    || promotion.getDiscountPercent().compareTo(HUNDRED) > 0) continue;
            BigDecimal discount = percent(base, promotion.getDiscountPercent()).min(base);
            int comparison = discount.compareTo(bestDiscount);
            if (discount.signum() > 0 && (comparison > 0 || (comparison == 0
                    && (best == null || promotion.getPromotionId() < best.getPromotionId())))) {
                bestDiscount = discount;
                best = promotion;
            }
        }
        return new PriceCalculation(base, base.subtract(bestDiscount), bestDiscount,
                best == null ? null : MarketingMapper.info(best));
    }

    public BigDecimal voucherDiscount(Voucher voucher, BigDecimal amount, LocalDateTime now) {
        if (!active(voucher.getStatus(), voucher.getStartDate(), voucher.getEndDate(), now)
                || voucher.getQuantity() <= 0 || amount.compareTo(voucher.getMinOrderAmount()) < 0
                || voucher.getDiscountValue().signum() <= 0
                || (voucher.getDiscountType() == DiscountType.PERCENT
                    && voucher.getDiscountValue().compareTo(HUNDRED) > 0)) {
            throw new BadRequestException("Voucher không hợp lệ hoặc không đủ điều kiện sử dụng");
        }
        BigDecimal discount = voucher.getDiscountType() == DiscountType.PERCENT
                ? percent(amount, voucher.getDiscountValue()) : voucher.getDiscountValue();
        if (voucher.getMaxDiscountAmount() != null) discount = discount.min(voucher.getMaxDiscountAmount());
        return discount.min(amount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        return checkedAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public BigDecimal checkedAmount(BigDecimal amount) {
        if (amount.signum() < 0 || amount.scale() > 2 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BadRequestException("Số tiền vượt giới hạn DECIMAL(14,2)");
        }
        return amount;
    }

    private static BigDecimal percent(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent).divide(HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean active(ActiveStatus status, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        return status == ActiveStatus.ACTIVE && !start.isAfter(now) && !end.isBefore(now);
    }

    private ProductVariantResponse response(ProductVariant variant, PriceCalculation price) {
        return new ProductVariantResponse(variant.getVariantId(), variant.getProduct().getProductId(),
                variant.getSku(), CatalogMapper.size(variant.getSize()), CatalogMapper.color(variant.getColor()),
                variant.getPrice(), variant.getQuantity(), variant.getStatus(), price.effectivePrice(), price.promotion());
    }
}
