package com.lauriel.store.service;

import com.lauriel.store.dto.request.CheckoutItemRequest;
import com.lauriel.store.dto.store.StoreCartItemView;
import com.lauriel.store.dto.store.StoreCartView;
import com.lauriel.store.entity.ProductVariant;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.repository.ProductVariantRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreCartService {
    private static final String CART = "storeCart";
    private static final String BUY_NOW = "storeBuyNow";
    private static final String VOUCHER = "storeVoucher";
    private final ProductVariantRepository variants;
    private final PricingService pricing;

    public StoreCartService(ProductVariantRepository variants, PricingService pricing) {
        this.variants = variants;
        this.pricing = pricing;
    }

    public void add(HttpSession session, Long variantId, int quantity) {
        ProductVariant variant = requirePurchasable(variantId);
        Map<Long, Integer> cart = cart(session, true);
        int total;
        try { total = Math.addExact(cart.getOrDefault(variantId, 0), quantity); }
        catch (ArithmeticException exception) { throw new BadRequestException("Số lượng không hợp lệ."); }
        validateQuantity(total, variant.getQuantity());
        cart.put(variantId, total);
    }

    public void buyNow(HttpSession session, Long variantId, int quantity) {
        ProductVariant variant = requirePurchasable(variantId);
        validateQuantity(quantity, variant.getQuantity());
        var state = new LinkedHashMap<Long, Integer>();
        state.put(variantId, quantity);
        session.setAttribute(BUY_NOW, state);
        session.removeAttribute(VOUCHER);
    }

    public void update(HttpSession session, Long variantId, int quantity) {
        Map<Long, Integer> cart = cart(session, false);
        if (!cart.containsKey(variantId)) throw new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng.");
        ProductVariant variant = requirePurchasable(variantId);
        validateQuantity(quantity, variant.getQuantity());
        cart.put(variantId, quantity);
    }

    public void remove(HttpSession session, Long variantId) {
        cart(session, false).remove(variantId);
        session.removeAttribute(VOUCHER);
    }

    public void clear(HttpSession session) {
        session.removeAttribute(CART);
        session.removeAttribute(VOUCHER);
    }

    public void clearBuyNow(HttpSession session) {
        session.removeAttribute(BUY_NOW);
        session.removeAttribute(VOUCHER);
    }

    public void selectVoucher(HttpSession session, String code) {
        if (code == null || code.isBlank()) session.removeAttribute(VOUCHER);
        else session.setAttribute(VOUCHER, VoucherService.normalizeCode(code));
    }

    public String selectedVoucher(HttpSession session) {
        Object value = session.getAttribute(VOUCHER);
        return value instanceof String code ? code : null;
    }

    public int count(HttpSession session) {
        return cart(session, false).values().stream().mapToInt(Integer::intValue).sum();
    }

    @Transactional(readOnly = true)
    public StoreCartView view(HttpSession session, boolean buyNow) {
        Map<Long, Integer> state = buyNow ? state(session, BUY_NOW) : cart(session, false);
        if (state.isEmpty()) return new StoreCartView(List.of(), 0, BigDecimal.ZERO.setScale(2));
        List<ProductVariant> found = variants.findByVariantIdIn(state.keySet());
        Map<Long, ProductVariant> indexed = new LinkedHashMap<>();
        found.forEach(variant -> indexed.put(variant.getVariantId(), variant));
        var prices = pricing.priceVariants(found, pricing.now());
        List<StoreCartItemView> items = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var entry : state.entrySet()) {
            ProductVariant variant = indexed.get(entry.getKey());
            if (variant == null || variant.getStatus() != ActiveStatus.ACTIVE
                    || variant.getProduct().getStatus() != ActiveStatus.ACTIVE) continue;
            int quantity = entry.getValue();
            var price = prices.get(variant.getVariantId());
            BigDecimal line = pricing.lineTotal(price.effectivePrice(), quantity);
            items.add(new StoreCartItemView(variant.getProduct().getProductId(), variant.getVariantId(),
                    variant.getProduct().getProductName(), variant.getProduct().getImgUrl(), variant.getSku(),
                    com.lauriel.store.mapper.CatalogMapper.size(variant.getSize()),
                    com.lauriel.store.mapper.CatalogMapper.color(variant.getColor()), variant.getPrice(),
                    price.effectivePrice(), price.promotion(), quantity, variant.getQuantity(), line));
            totalQuantity += quantity;
            subtotal = pricing.checkedAmount(subtotal.add(line));
        }
        return new StoreCartView(List.copyOf(items), totalQuantity, subtotal);
    }

    public List<CheckoutItemRequest> checkoutItems(HttpSession session, boolean buyNow) {
        return (buyNow ? state(session, BUY_NOW) : cart(session, false)).entrySet().stream()
                .map(entry -> new CheckoutItemRequest(entry.getKey(), entry.getValue())).toList();
    }

    private ProductVariant requirePurchasable(Long variantId) {
        if (variantId == null || variantId <= 0) throw new BadRequestException("Biến thể không hợp lệ.");
        ProductVariant variant = variants.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm."));
        if (variant.getStatus() != ActiveStatus.ACTIVE || variant.getProduct().getStatus() != ActiveStatus.ACTIVE) {
            throw new BadRequestException("Sản phẩm không còn hoạt động.");
        }
        return variant;
    }

    private static void validateQuantity(int quantity, int stock) {
        if (quantity <= 0) throw new BadRequestException("Số lượng phải lớn hơn 0.");
        if (quantity > stock) throw new BadRequestException("Số lượng vượt quá tồn kho hiện tại.");
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> cart(HttpSession session, boolean create) {
        Object value = session.getAttribute(CART);
        if (value instanceof Map<?, ?>) return (Map<Long, Integer>) value;
        if (!create) return new LinkedHashMap<>();
        var result = new LinkedHashMap<Long, Integer>();
        session.setAttribute(CART, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> state(HttpSession session, String name) {
        Object value = session.getAttribute(name);
        return value instanceof Map<?, ?> ? (Map<Long, Integer>) value : Map.of();
    }
}
