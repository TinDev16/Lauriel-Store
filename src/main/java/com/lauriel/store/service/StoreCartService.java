package com.lauriel.store.service;

import com.lauriel.store.dto.request.CartSnapshotRequest;
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

    public static final String CART_SESSION_ATTRIBUTE = "storeCart";
    public static final String CART_OWNER_SESSION_ATTRIBUTE = "storeCartOwner";
    private static final String CART = CART_SESSION_ATTRIBUTE;
    private static final String CART_OWNER = CART_OWNER_SESSION_ATTRIBUTE;
    private static final Long ANONYMOUS_OWNER = -1L;
    private static final String BUY_NOW = "storeBuyNow";
    private static final String VOUCHER = "storeVoucher";
    private final ProductVariantRepository variants;
    private final PricingService pricing;
    private final CustomerEngagementService engagement;

    public StoreCartService(
        ProductVariantRepository variants,
        PricingService pricing,
        CustomerEngagementService engagement
    ) {
        this.variants = variants;
        this.pricing = pricing;
        this.engagement = engagement;
    }

    public void add(HttpSession session, Long variantId, int quantity) {
        add(session, variantId, quantity, null);
    }

    public void add(
        HttpSession session,
        Long variantId,
        int quantity,
        Long userId
    ) {
        prepareOwner(session, userId);
        ProductVariant variant = requirePurchasable(variantId);
        Map<Long, Integer> cart = cart(session, true);
        int total;
        try {
            total = Math.addExact(cart.getOrDefault(variantId, 0), quantity);
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Số lượng không hợp lệ.");
        }
        validateQuantity(total, variant.getQuantity());
        cart.put(variantId, total);
        persist(session, userId);
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
        update(session, variantId, quantity, null);
    }

    public void update(
        HttpSession session,
        Long variantId,
        int quantity,
        Long userId
    ) {
        prepareOwner(session, userId);
        Map<Long, Integer> cart = cart(session, false);
        if (!cart.containsKey(variantId)) throw new ResourceNotFoundException(
            "Sản phẩm không có trong giỏ hàng."
        );
        ProductVariant variant = requirePurchasable(variantId);
        validateQuantity(quantity, variant.getQuantity());
        cart.put(variantId, quantity);
        persist(session, userId);
    }

    public void remove(HttpSession session, Long variantId) {
        remove(session, variantId, null);
    }

    public void remove(HttpSession session, Long variantId, Long userId) {
        prepareOwner(session, userId);
        cart(session, false).remove(variantId);
        session.removeAttribute(VOUCHER);
        persist(session, userId);
    }

    public void clear(HttpSession session) {
        clear(session, null);
    }

    public void clear(HttpSession session, Long userId) {
        prepareOwner(session, userId);
        session.removeAttribute(CART);
        session.removeAttribute(VOUCHER);
        persist(session, userId);
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
        return cart(session, false)
            .values()
            .stream()
            .mapToInt(Integer::intValue)
            .sum();
    }

    /** Clears account-specific session state while retaining the anonymous shopping cart. */
    public static boolean retainCartOnly(HttpSession session) {
        if (session == null) return false;
        Object cart = session.getAttribute(CART);
        Object owner = session.getAttribute(CART_OWNER);
        java.util.Collections.list(session.getAttributeNames()).forEach(
            session::removeAttribute
        );
        if (cart != null) {
            session.setAttribute(CART, cart);
            if (owner != null) session.setAttribute(CART_OWNER, owner);
            return true;
        }
        return false;
    }

    public void activateCustomerCart(HttpSession session, Long userId) {
        if (session == null || userId == null) return;
        Object owner = session.getAttribute(CART_OWNER);
        Map<Long, Integer> current = new LinkedHashMap<>(cart(session, false));
        Map<Long, Integer> accountCart = new LinkedHashMap<>(
            engagement.loadCart(userId)
        );

        if (userId.equals(owner)) {
            accountCart.clear();
            accountCart.putAll(current);
        } else if (ANONYMOUS_OWNER.equals(owner)) {
            current.forEach((variantId, quantity) ->
                accountCart.merge(variantId, quantity, Math::max)
            );
        }

        session.setAttribute(CART, accountCart);
        session.setAttribute(CART_OWNER, userId);
        persist(session, userId);
    }

    @Transactional(readOnly = true)
    public StoreCartView view(HttpSession session, boolean buyNow) {
        Map<Long, Integer> state = buyNow
            ? state(session, BUY_NOW)
            : cart(session, false);
        if (state.isEmpty()) return new StoreCartView(
            List.of(),
            0,
            BigDecimal.ZERO.setScale(2)
        );
        List<ProductVariant> found = variants.findByVariantIdIn(state.keySet());
        Map<Long, ProductVariant> indexed = new LinkedHashMap<>();
        found.forEach(variant -> indexed.put(variant.getVariantId(), variant));
        var prices = pricing.priceVariants(found, pricing.now());
        List<StoreCartItemView> items = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var entry : state.entrySet()) {
            ProductVariant variant = indexed.get(entry.getKey());
            if (
                variant == null ||
                variant.getStatus() != ActiveStatus.ACTIVE ||
                variant.getProduct().getStatus() != ActiveStatus.ACTIVE
            ) continue;
            int quantity = entry.getValue();
            var price = prices.get(variant.getVariantId());
            BigDecimal line = pricing.lineTotal(
                price.effectivePrice(),
                quantity
            );
            items.add(
                new StoreCartItemView(
                    variant.getProduct().getProductId(),
                    variant.getVariantId(),
                    variant.getProduct().getProductName(),
                    variant.getProduct().getImgUrl(),
                    variant.getSku(),
                    com.lauriel.store.mapper.CatalogMapper.size(
                        variant.getSize()
                    ),
                    com.lauriel.store.mapper.CatalogMapper.color(
                        variant.getColor()
                    ),
                    variant.getPrice(),
                    price.effectivePrice(),
                    price.promotion(),
                    quantity,
                    variant.getQuantity(),
                    line
                )
            );
            totalQuantity += quantity;
            subtotal = pricing.checkedAmount(subtotal.add(line));
        }
        return new StoreCartView(List.copyOf(items), totalQuantity, subtotal);
    }

    public List<CheckoutItemRequest> checkoutItems(
        HttpSession session,
        boolean buyNow
    ) {
        return (buyNow ? state(session, BUY_NOW) : cart(session, false))
            .entrySet()
            .stream()
            .map(entry ->
                new CheckoutItemRequest(entry.getKey(), entry.getValue())
            )
            .toList();
    }

    private ProductVariant requirePurchasable(Long variantId) {
        if (variantId == null || variantId <= 0) throw new BadRequestException(
            "Thuộc tính không hợp lệ."
        );
        ProductVariant variant = variants
            .findById(variantId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Không tìm thấy thuộc tính sản phẩm."
                )
            );
        if (
            variant.getStatus() != ActiveStatus.ACTIVE ||
            variant.getProduct().getStatus() != ActiveStatus.ACTIVE
        ) {
            throw new BadRequestException("Sản phẩm không còn hoạt động.");
        }
        return variant;
    }

    private static void validateQuantity(int quantity, int stock) {
        if (quantity <= 0) throw new BadRequestException(
            "Số lượng phải lớn hơn 0."
        );
        if (quantity > stock) throw new BadRequestException(
            "Số lượng vượt quá tồn kho hiện tại."
        );
    }

    private void prepareOwner(HttpSession session, Long userId) {
        Object owner = session.getAttribute(CART_OWNER);
        if (userId != null && !userId.equals(owner)) {
            activateCustomerCart(session, userId);
        } else if (userId == null && owner == null) {
            session.setAttribute(CART_OWNER, ANONYMOUS_OWNER);
        }
    }

    private void persist(HttpSession session, Long userId) {
        if (userId == null) return;
        List<CheckoutItemRequest> items = cart(session, false)
            .entrySet()
            .stream()
            .map(entry ->
                new CheckoutItemRequest(entry.getKey(), entry.getValue())
            )
            .toList();
        engagement.syncCart(userId, new CartSnapshotRequest(items));
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
        return value instanceof Map<?, ?>
            ? (Map<Long, Integer>) value
            : Map.of();
    }
}
