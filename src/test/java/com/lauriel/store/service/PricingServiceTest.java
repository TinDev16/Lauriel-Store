package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lauriel.store.dto.response.PriceCalculation;
import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.*;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PricingServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 18, 10, 0);
    private final OrderPromotionRepository orderPromotions = mock(OrderPromotionRepository.class);
    private final PricingService pricing = new PricingService(mock(PromotionProductRepository.class), mock(PromotionVariantRepository.class),
            mock(ProductVariantRepository.class), orderPromotions, Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    @Test
    void choosesGreatestActualOrderPromotionDiscountThenThresholdThenLowestId() {
        var capped = orderPromotion(1, "100000", "30", "20000");
        var better = orderPromotion(2, "150000", "15", null);
        when(orderPromotions.findEligible(ActiveStatus.ACTIVE, NOW, new BigDecimal("200000.00")))
                .thenReturn(List.of(capped, better));

        var result = pricing.orderPromotion(new BigDecimal("200000.00"), NOW);

        assertThat(result.promotion()).isSameAs(better);
        assertThat(result.discount()).isEqualByComparingTo("30000.00");
        assertThat(result.amountAfter(new BigDecimal("200000.00"))).isEqualByComparingTo("170000.00");
    }

    private OrderPromotion orderPromotion(long id, String minimum, String percent, String cap) {
        var promotion = new OrderPromotion(); promotion.setOrderPromotionId(id);
        promotion.setMinOrderAmount(new BigDecimal(minimum)); promotion.setDiscountPercent(new BigDecimal(percent));
        promotion.setMaxDiscountAmount(cap == null ? null : new BigDecimal(cap));
        promotion.setStatus(ActiveStatus.ACTIVE); promotion.setStartDate(NOW.minusDays(1)); promotion.setEndDate(NOW.plusDays(1));
        return promotion;
    }

    @Test
    void capsOrderPromotionAndIgnoresInactiveFutureAndExpiredCandidates() {
        var capped = orderPromotion(1, "0", "10", "150000");
        var inactive = orderPromotion(2, "0", "90", null); inactive.setStatus(ActiveStatus.INACTIVE);
        var future = orderPromotion(3, "0", "90", null); future.setStartDate(NOW.plusMinutes(1));
        var expired = orderPromotion(4, "0", "90", null); expired.setEndDate(NOW.minusMinutes(1));
        var subtotal = new BigDecimal("3000000.00");
        when(orderPromotions.findEligible(ActiveStatus.ACTIVE, NOW, subtotal))
                .thenReturn(List.of(inactive, future, expired, capped));

        var result = pricing.orderPromotion(subtotal, NOW);

        assertThat(result.promotion()).isSameAs(capped);
        assertThat(result.discount()).isEqualByComparingTo("150000.00");
    }

    @Test
    void thresholdPromotionCapsTenPercentOfOnePointTwoMillionAtFiftyThousand() {
        var promotion = orderPromotion(9, "1000000", "10", "50000");
        var subtotal = new BigDecimal("1200000.00");
        when(orderPromotions.findEligible(ActiveStatus.ACTIVE, NOW, subtotal))
                .thenReturn(List.of(promotion));

        var result = pricing.orderPromotion(subtotal, NOW);

        assertThat(result.discount()).isEqualByComparingTo("50000.00");
        assertThat(result.amountAfter(subtotal)).isEqualByComparingTo("1150000.00");
        assertThat(result.amountAfter(subtotal).add(new BigDecimal("20000")))
                .isEqualByComparingTo("1170000.00");
    }

    @Test
    void subtotalBelowThresholdHasNoOrderPromotionDiscount() {
        var subtotal = new BigDecimal("999999.00");
        when(orderPromotions.findEligible(ActiveStatus.ACTIVE, NOW, subtotal)).thenReturn(List.of());

        var result = pricing.orderPromotion(subtotal, NOW);

        assertThat(result.promotion()).isNull();
        assertThat(result.discount()).isEqualByComparingTo("0.00");
        assertThat(result.amountAfter(subtotal)).isEqualByComparingTo("999999.00");
    }

    private Promotion promotion(long id, String percent) {
        var promotion = new Promotion();
        promotion.setPromotionId(id);
        promotion.setPromotionName("Promotion " + id);
        promotion.setDiscountPercent(new BigDecimal(percent));
        promotion.setStartDate(NOW.minusDays(1));
        promotion.setEndDate(NOW.plusDays(1));
        promotion.setStatus(ActiveStatus.ACTIVE);
        return promotion;
    }

    private Voucher voucher(DiscountType type, String value) {
        var voucher = new Voucher();
        voucher.setCode("TEST");
        voucher.setDiscountType(type);
        voucher.setDiscountValue(new BigDecimal(value));
        voucher.setMinOrderAmount(BigDecimal.ZERO);
        voucher.setQuantity(1);
        voucher.setStatus(ActiveStatus.ACTIVE);
        voucher.setStartDate(NOW.minusDays(1));
        voucher.setEndDate(NOW.plusDays(1));
        return voucher;
    }

    @Test
    void noPromotionKeepsBasePrice() {
        var result = pricing.bestPrice(new BigDecimal("300"), List.of(), NOW);
        assertThat(result.effectivePrice()).isEqualByComparingTo("300");
        assertThat(result.promotion()).isNull();
    }

    @Test
    void percentageUsesHalfUpRoundingAtUnitLevel() {
        var result = pricing.bestPrice(new BigDecimal("0.05"), List.of(promotion(1, "10")), NOW);
        assertThat(result.promotionDiscount()).isEqualByComparingTo("0.01");
        assertThat(result.effectivePrice()).isEqualByComparingTo("0.04");
        assertThat(pricing.lineTotal(result.effectivePrice(), 3)).isEqualByComparingTo("0.12");
    }

    @Test
    void hundredPercentNeverMakesNegativePrice() {
        var result = pricing.bestPrice(new BigDecimal("300"), List.of(promotion(1, "100")), NOW);
        assertThat(result.effectivePrice()).isEqualByComparingTo("0");
    }

    @Test
    void bestPromotionIsChosenWithoutStackingAndTieUsesLowestId() {
        var result = pricing.bestPrice(new BigDecimal("300"),
                List.of(promotion(3, "20"), promotion(1, "20"), promotion(2, "10")), NOW);
        assertThat(result.effectivePrice()).isEqualByComparingTo("240");
        assertThat(result.promotion().promotionId()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"future", "expired", "inactive", "zero"})
    void invalidOrZeroBenefitPromotionIsIgnored(String kind) {
        var promotion = promotion(1, "10");
        if (kind.equals("future")) promotion.setStartDate(NOW.plusSeconds(1));
        if (kind.equals("expired")) promotion.setEndDate(NOW.minusSeconds(1));
        if (kind.equals("inactive")) promotion.setStatus(ActiveStatus.INACTIVE);
        if (kind.equals("zero")) promotion.setDiscountPercent(BigDecimal.ZERO);
        assertThat(pricing.bestPrice(new BigDecimal("300"), List.of(promotion), NOW).effectivePrice())
                .isEqualByComparingTo("300");
    }

    @Test
    void promotionAndVoucherTimeBoundariesAreInclusive() {
        var promotion = promotion(1, "10");
        promotion.setStartDate(NOW);
        promotion.setEndDate(NOW.plusSeconds(1));
        assertThat(pricing.bestPrice(new BigDecimal("100"), List.of(promotion), NOW).effectivePrice()).isEqualByComparingTo("90");
        assertThat(pricing.bestPrice(new BigDecimal("100"), List.of(promotion), NOW.plusSeconds(1)).effectivePrice()).isEqualByComparingTo("90");
        var voucher = voucher(DiscountType.PERCENT, "10");
        voucher.setStartDate(NOW);
        voucher.setEndDate(NOW.plusSeconds(1));
        assertThat(pricing.voucherDiscount(voucher, new BigDecimal("100"), NOW)).isEqualByComparingTo("10");
        assertThat(pricing.voucherDiscount(voucher, new BigDecimal("100"), NOW.plusSeconds(1))).isEqualByComparingTo("10");
    }

    @Test
    void voucherPercentRespectsCap() {
        var voucher = voucher(DiscountType.PERCENT, "20");
        voucher.setMaxDiscountAmount(new BigDecimal("30"));
        assertThat(pricing.voucherDiscount(voucher, new BigDecimal("300"), NOW)).isEqualByComparingTo("30");
    }

    @Test
    void fixedVoucherIsClampedToAmountAndZeroCapIsAllowedBySchema() {
        var voucher = voucher(DiscountType.FIXED, "500");
        assertThat(pricing.voucherDiscount(voucher, new BigDecimal("300"), NOW)).isEqualByComparingTo("300");
        voucher.setMaxDiscountAmount(BigDecimal.ZERO);
        assertThat(pricing.voucherDiscount(voucher, new BigDecimal("300"), NOW)).isEqualByComparingTo("0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"inactive", "future", "expired", "exhausted", "minimum", "percent", "zero"})
    void ineligibleVoucherIs400(String condition) {
        var voucher = voucher(DiscountType.PERCENT, "10");
        switch (condition) {
            case "inactive" -> voucher.setStatus(ActiveStatus.INACTIVE);
            case "future" -> voucher.setStartDate(NOW.plusSeconds(1));
            case "expired" -> voucher.setEndDate(NOW.minusSeconds(1));
            case "exhausted" -> voucher.setQuantity(0);
            case "minimum" -> voucher.setMinOrderAmount(new BigDecimal("301"));
            case "percent" -> voucher.setDiscountValue(new BigDecimal("150"));
            case "zero" -> voucher.setDiscountValue(BigDecimal.ZERO);
        }
        assertThatThrownBy(() -> pricing.voucherDiscount(voucher, new BigDecimal("300"), NOW))
                .isInstanceOf(BadRequestException.class);
    }
}
