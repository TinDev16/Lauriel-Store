package com.lauriel.store.service;

import com.lauriel.store.config.StoreCommerceProperties;
import com.lauriel.store.dto.request.*;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.exception.*;
import com.lauriel.store.mapper.MarketingMapper;
import com.lauriel.store.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class VoucherService {

    public record AppliedVoucher(Voucher voucher, BigDecimal discount) {}

    private final VoucherRepository vouchers;
    private final ProductVariantRepository variants;
    private final PricingService pricing;
    private final StoreCommerceProperties commerce;

    public VoucherService(
        VoucherRepository vouchers,
        ProductVariantRepository variants,
        PricingService pricing,
        StoreCommerceProperties commerce
    ) {
        this.vouchers = vouchers;
        this.variants = variants;
        this.pricing = pricing;
        this.commerce = commerce;
    }

    public static String normalizeCode(String code) {
        return code.strip().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> list(int page, int size) {
        return list(page, size, null, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> list(
        int page,
        int size,
        String keyword,
        ActiveStatus status,
        com.lauriel.store.entity.enums.DiscountType discountType
    ) {
        return list(page, size, keyword, status, discountType, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> list(
        int page,
        int size,
        String keyword,
        ActiveStatus status,
        com.lauriel.store.entity.enums.DiscountType discountType,
        String timeFilter
    ) {
        String term =
            keyword == null || keyword.isBlank()
                ? null
                : "%" + keyword.strip().toLowerCase(Locale.ROOT) + "%";
        var now = pricing.now();
        org.springframework.data.jpa.domain.Specification<Voucher> filter = (
            root,
            query,
            builder
        ) -> {
            var predicates =
                new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) predicates.add(
                builder.equal(root.get("status"), status)
            );
            if ("RUNNING".equalsIgnoreCase(timeFilter)) {
                predicates.add(builder.equal(root.get("status"), ActiveStatus.ACTIVE));
                predicates.add(builder.greaterThan(root.get("quantity"), 0));
                predicates.add(builder.or(
                    builder.isNull(root.get("startDate")),
                    builder.lessThanOrEqualTo(root.get("startDate"), now)
                ));
                predicates.add(builder.or(
                    builder.isNull(root.get("endDate")),
                    builder.greaterThanOrEqualTo(root.get("endDate"), now)
                ));
            } else if ("OUT_OF_STOCK".equalsIgnoreCase(timeFilter)) {
                predicates.add(builder.equal(root.get("status"), ActiveStatus.ACTIVE));
                predicates.add(builder.or(
                    builder.isNull(root.get("quantity")),
                    builder.lessThanOrEqualTo(root.get("quantity"), 0)
                ));
            } else if ("UPCOMING".equalsIgnoreCase(timeFilter)) {
                predicates.add(builder.equal(root.get("status"), ActiveStatus.ACTIVE));
                predicates.add(builder.greaterThan(root.get("startDate"), now));
            } else if ("EXPIRED".equalsIgnoreCase(timeFilter)) {
                predicates.add(builder.lessThan(root.get("endDate"), now));
            } else if ("INACTIVE".equalsIgnoreCase(timeFilter)) {
                predicates.add(builder.equal(root.get("status"), ActiveStatus.INACTIVE));
            }
            if (discountType != null) predicates.add(
                builder.equal(root.get("discountType"), discountType)
            );
            if (term != null) predicates.add(
                builder.or(
                    builder.like(builder.lower(root.get("code")), term),
                    builder.like(builder.lower(root.get("voucherName")), term)
                )
            );
            return builder.and(
                predicates.toArray(
                    jakarta.persistence.criteria.Predicate[]::new
                )
            );
        };
        return InventoryPagination.response(
            vouchers.findAll(
                filter,
                InventoryPagination.page(
                    page,
                    size,
                    Sort.by(
                        Sort.Order.asc("status"),
                        Sort.Order.asc("voucherId")
                    )
                )
            ),
            MarketingMapper::voucher
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> listAvailable(int page, int size) {
        return InventoryPagination.response(
            vouchers.findAvailable(
                ActiveStatus.ACTIVE,
                pricing.now(),
                InventoryPagination.page(page, size, Sort.unsorted())
            ),
            MarketingMapper::voucher
        );
    }

    @Transactional(readOnly = true)
    public VoucherResponse detail(Long id) {
        return MarketingMapper.voucher(
            vouchers
                .findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Không tìm thấy Voucher")
                )
        );
    }

    @Transactional
    public VoucherResponse create(VoucherRequest request) {
        var voucher = new Voucher();
        apply(voucher, request);
        return MarketingMapper.voucher(vouchers.saveAndFlush(voucher));
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherRequest request) {
        var voucher = vouchers
            .findByIdForUpdate(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy Voucher")
            );
        apply(voucher, request);
        return MarketingMapper.voucher(vouchers.saveAndFlush(voucher));
    }

    @Transactional
    public VoucherResponse updateStatus(Long id, ActiveStatus status) {
        var voucher = vouchers
            .findByIdForUpdate(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy mã giảm giá")
            );
        voucher.setStatus(status);
        return MarketingMapper.voucher(vouchers.saveAndFlush(voucher));
    }

    @Transactional(readOnly = true)
    public VoucherValidationResponse preview(VoucherValidationRequest request) {
        return previewAmounts(request.items(), request.code());
    }

    @Transactional(readOnly = true)
    public VoucherValidationResponse preview(OrderPreviewRequest request) {
        return previewAmounts(request.items(), request.voucherCode());
    }

    private VoucherValidationResponse previewAmounts(
        List<CheckoutItemRequest> requestedItems,
        String requestedCode
    ) {
        var quantities = CheckoutItems.normalize(requestedItems);
        var found = variants.findByVariantIdIn(quantities.keySet());
        if (
            found.size() != quantities.size()
        ) throw new ResourceNotFoundException("Không tìm thấy thuộc tính");
        if (
            found
                .stream()
                .anyMatch(
                    v ->
                        v.getStatus() != ActiveStatus.ACTIVE ||
                        v.getProduct().getStatus() != ActiveStatus.ACTIVE
                )
        ) {
            throw new BadRequestException("Sản phẩm không còn hoạt động");
        }
        var now = pricing.now();
        var prices = pricing.priceVariants(found, now);
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal productPromotionDiscount = BigDecimal.ZERO;
        for (var variant : found) {
            amount = pricing.checkedAmount(
                amount.add(
                    pricing.lineTotal(
                        prices.get(variant.getVariantId()).effectivePrice(),
                        quantities.get(variant.getVariantId())
                    )
                )
            );
            productPromotionDiscount = pricing.checkedAmount(
                productPromotionDiscount.add(
                    pricing.lineTotal(
                        prices.get(variant.getVariantId()).promotionDiscount(),
                        quantities.get(variant.getVariantId())
                    )
                )
            );
        }
        var threshold = pricing.orderPromotion(amount, now);
        var amountAfterOrderPromotion = threshold.amountAfter(amount);
        Voucher voucher = null;
        BigDecimal discount = BigDecimal.ZERO.setScale(2);
        if (requestedCode != null && !requestedCode.isBlank()) {
            voucher = vouchers
                .findByCodeIgnoreCase(normalizeCode(requestedCode))
                .orElseThrow(() ->
                    new BadRequestException(
                        "Voucher không hợp lệ hoặc không đủ điều kiện sử dụng"
                    )
                );
            discount = pricing.voucherDiscount(
                voucher,
                amountAfterOrderPromotion,
                now
            );
        }
        var shipping = pricing.checkedAmount(commerce.getShippingFee());
        return new VoucherValidationResponse(
            true,
            voucher == null ? null : voucher.getCode(),
            voucher == null ? null : voucher.getVoucherName(),
            amount,
            amount,
            productPromotionDiscount,
            threshold.promotion() == null
                ? null
                : threshold.promotion().getOrderPromotionId(),
            threshold.promotion() == null
                ? null
                : threshold.promotion().getPromotionName(),
            threshold.promotion() == null
                ? null
                : threshold.promotion().getDiscountPercent(),
            threshold.discount(),
            discount,
            shipping,
            pricing.checkedAmount(
                amountAfterOrderPromotion.subtract(discount).add(shipping)
            )
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AppliedVoucher consume(
        String requestedCode,
        BigDecimal amount,
        LocalDateTime now
    ) {
        if (
            requestedCode == null || requestedCode.isBlank()
        ) return new AppliedVoucher(null, BigDecimal.ZERO);
        var voucher = vouchers
            .findByCodeForUpdate(normalizeCode(requestedCode))
            .orElseThrow(() ->
                new BadRequestException(
                    "Voucher không hợp lệ hoặc không đủ điều kiện sử dụng"
                )
            );
        var discount = pricing.voucherDiscount(voucher, amount, now);
        voucher.setQuantity(voucher.getQuantity() - 1);
        vouchers.saveAndFlush(voucher);
        return new AppliedVoucher(voucher, discount);
    }

    // Called only after the order and all restocked variants have been locked.
    @Transactional(propagation = Propagation.MANDATORY)
    public void restore(Long voucherId) {
        if (voucherId == null) return;
        var voucher = vouchers
            .findByIdForUpdate(voucherId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy Voucher")
            );
        if (voucher.getQuantity() == Integer.MAX_VALUE) {
            throw new ResourceConflictException(
                "Số lượt Voucher vượt giới hạn Integer"
            );
        }
        voucher.setQuantity(voucher.getQuantity() + 1); // Restore even when expired or inactive.
        vouchers.saveAndFlush(voucher);
    }

    private void apply(Voucher voucher, VoucherRequest request) {
        String code = normalizeCode(request.code());
        if (code.isEmpty() || code.length() > 50) throw new BadRequestException(
            "Code Voucher không hợp lệ"
        );
        boolean duplicate =
            voucher.getVoucherId() == null
                ? vouchers.existsByCodeIgnoreCase(code)
                : vouchers.existsByCodeIgnoreCaseAndVoucherIdNot(
                      code,
                      voucher.getVoucherId()
                  );
        if (duplicate) throw new DuplicateResourceException(
            "Code Voucher đã tồn tại"
        );
        voucher.setCode(code);
        voucher.setVoucherName(request.voucherName().strip());
        voucher.setDiscountType(request.discountType());
        voucher.setDiscountValue(request.discountValue());
        voucher.setMinOrderAmount(request.minOrderAmount());
        voucher.setMaxDiscountAmount(request.maxDiscountAmount());
        voucher.setQuantity(request.quantity());
        voucher.setStartDate(request.startDate());
        voucher.setEndDate(request.endDate());
        voucher.setStatus(request.status());
    }
}
