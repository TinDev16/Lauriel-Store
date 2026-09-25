package com.lauriel.store.service;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.config.StoreCommerceProperties;
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

    public VoucherService(VoucherRepository vouchers, ProductVariantRepository variants, PricingService pricing,
            StoreCommerceProperties commerce) {
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
        return InventoryPagination.response(vouchers.findAll(InventoryPagination.page(page, size, Sort.by("voucherId"))),
                MarketingMapper::voucher);
    }

    @Transactional(readOnly = true)
    public VoucherResponse detail(Long id) {
        return MarketingMapper.voucher(vouchers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Voucher")));
    }

    @Transactional
    public VoucherResponse create(VoucherRequest request) {
        var voucher = new Voucher();
        apply(voucher, request);
        return MarketingMapper.voucher(vouchers.saveAndFlush(voucher));
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherRequest request) {
        var voucher = vouchers.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Voucher"));
        apply(voucher, request);
        return MarketingMapper.voucher(vouchers.saveAndFlush(voucher));
    }

    @Transactional
    public VoucherResponse updateStatus(Long id,ActiveStatus status) {
        var voucher=vouchers.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá"));
        voucher.setStatus(status);
        return MarketingMapper.voucher(vouchers.saveAndFlush(voucher));
    }

    @Transactional(readOnly = true)
    public VoucherValidationResponse preview(VoucherValidationRequest request) {
        var quantities = CheckoutItems.normalize(request.items());
        // Read-only preview deliberately takes no inventory/voucher write locks and does not reserve anything.
        var found = variants.findByVariantIdIn(quantities.keySet());
        if (found.size() != quantities.size()) throw new ResourceNotFoundException("Không tìm thấy biến thể");
        if (found.stream().anyMatch(v -> v.getStatus() != ActiveStatus.ACTIVE
                || v.getProduct().getStatus() != ActiveStatus.ACTIVE)) {
            throw new BadRequestException("Sản phẩm không còn hoạt động");
        }
        var now = pricing.now();
        var prices = pricing.priceVariants(found, now);
        BigDecimal amount = BigDecimal.ZERO;
        for (var variant : found) {
            amount = pricing.checkedAmount(amount.add(pricing.lineTotal(
                    prices.get(variant.getVariantId()).effectivePrice(), quantities.get(variant.getVariantId()))));
        }
        String code = normalizeCode(request.code());
        var voucher = vouchers.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BadRequestException("Voucher không hợp lệ hoặc không đủ điều kiện sử dụng"));
        var discount = pricing.voucherDiscount(voucher, amount, now);
        var shipping = pricing.checkedAmount(commerce.getShippingFee());
        return new VoucherValidationResponse(true, voucher.getCode(), amount, discount, shipping,
                pricing.checkedAmount(amount.subtract(discount).add(shipping)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AppliedVoucher consume(String requestedCode, BigDecimal amount, LocalDateTime now) {
        if (requestedCode == null || requestedCode.isBlank()) return new AppliedVoucher(null, BigDecimal.ZERO);
        var voucher = vouchers.findByCodeForUpdate(normalizeCode(requestedCode))
                .orElseThrow(() -> new BadRequestException("Voucher không hợp lệ hoặc không đủ điều kiện sử dụng"));
        var discount = pricing.voucherDiscount(voucher, amount, now);
        voucher.setQuantity(voucher.getQuantity() - 1);
        vouchers.saveAndFlush(voucher);
        return new AppliedVoucher(voucher, discount);
    }

    // Called only after the order and all restocked variants have been locked.
    @Transactional(propagation = Propagation.MANDATORY)
    public void restore(Long voucherId) {
        if (voucherId == null) return;
        var voucher = vouchers.findByIdForUpdate(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Voucher"));
        if (voucher.getQuantity() == Integer.MAX_VALUE) {
            throw new ResourceConflictException("Số lượt Voucher vượt giới hạn Integer");
        }
        voucher.setQuantity(voucher.getQuantity() + 1); // Restore even when expired or inactive.
        vouchers.saveAndFlush(voucher);
    }

    private void apply(Voucher voucher, VoucherRequest request) {
        String code = normalizeCode(request.code());
        if (code.isEmpty() || code.length() > 50) throw new BadRequestException("Code Voucher không hợp lệ");
        boolean duplicate = voucher.getVoucherId() == null ? vouchers.existsByCodeIgnoreCase(code)
                : vouchers.existsByCodeIgnoreCaseAndVoucherIdNot(code, voucher.getVoucherId());
        if (duplicate) throw new DuplicateResourceException("Code Voucher đã tồn tại");
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
