package com.lauriel.store.service;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.config.StoreCommerceProperties;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.Order;
import com.lauriel.store.entity.OrderDetail;
import com.lauriel.store.entity.OrderDetailId;
import com.lauriel.store.entity.Customer;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.event.OrderCreatedEvent;
import com.lauriel.store.exception.*;
import com.lauriel.store.mapper.OrderMapper;
import com.lauriel.store.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final OrderDetailRepository details;
    private final CustomerRepository customers;
    private final UserRepository users;
    private final InventoryService inventory;
    private final PricingService pricing;
    private final VoucherService vouchers;
    private final StoreCommerceProperties commerce;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, OrderDetailRepository details, CustomerRepository customers,
            UserRepository users, InventoryService inventory, PricingService pricing, VoucherService vouchers,
            StoreCommerceProperties commerce, ApplicationEventPublisher events) {
        this.orders = orders;
        this.details = details;
        this.customers = customers;
        this.users = users;
        this.inventory = inventory;
        this.pricing = pricing;
        this.vouchers = vouchers;
        this.commerce = commerce;
        this.events = events;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request, Long actorId) {
        var customer = requireCustomer(actorId);
        var quantities = CheckoutItems.normalize(request.items());
        // Locks and verifies stock once, before reading prices or creating any order rows.
        var locked = inventory.lockForSale(quantities);
        var now = pricing.now();
        var prices = pricing.priceVariants(locked.values(), now);
        var order = new Order();
        order.setCustomer(customer);
        order.setVoucher(null);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.shippingAddress().strip());
        var lines = new ArrayList<OrderDetail>();
        BigDecimal total = BigDecimal.ZERO;
        for (var entry : quantities.entrySet()) {
            var variant = locked.get(entry.getKey());
            var item = new OrderDetail();
            item.setOrder(order);
            item.setProductVariant(variant);
            item.setQuantity(entry.getValue());
            var price = prices.get(variant.getVariantId());
            item.setUnitPrice(price.effectivePrice()); // Snapshot after the best product promotion.
            item.setDiscountAmount(price.promotionDiscount()); // Per-unit promotion discount snapshot.
            item.setSubtotal(pricing.lineTotal(price.effectivePrice(), entry.getValue()));
            total = pricing.checkedAmount(total.add(item.getSubtotal()));
            lines.add(item);
        }
        var threshold = pricing.orderPromotion(total, now);
        var amountAfterOrderPromotion = threshold.amountAfter(total);
        var applied = vouchers.consume(request.voucherCode(), amountAfterOrderPromotion, now);
        order.setVoucher(applied.voucher());
        order.setOrderPromotion(threshold.promotion());
        order.setOrderPromotionName(threshold.promotion() == null ? null : threshold.promotion().getPromotionName());
        order.setOrderPromotionPercent(threshold.promotion() == null ? null : threshold.promotion().getDiscountPercent());
        order.setOrderPromotionDiscount(threshold.discount());
        order.setTotalAmount(total);
        order.setDiscountAmount(applied.discount());
        order.setShippingFee(pricing.checkedAmount(commerce.getShippingFee()));
        order.setFinalAmount(pricing.checkedAmount(amountAfterOrderPromotion.subtract(applied.discount()).add(order.getShippingFee())));
        orders.saveAndFlush(order);
        for (var item : lines) {
            item.setId(new OrderDetailId(order.getOrderId(), item.getProductVariant().getVariantId()));
            details.saveAndFlush(item);
        }
        inventory.decreaseForSale(order.getOrderId(), lines, customer.getUser());
        var result = response(order);
        events.publishEvent(OrderCreatedEvent.snapshot(order, lines));
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> myOrders(Long actorId, int page, int size, OrderStatus status) {
        var customer = requireCustomer(actorId);
        var pageable = InventoryPagination.page(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "orderId"));
        var result = status == null
                ? orders.findByCustomer_CustomerId(customer.getCustomerId(), pageable)
                : orders.findByCustomer_CustomerIdAndStatus(customer.getCustomerId(), status, pageable);
        return InventoryPagination.response(result, OrderMapper::summary);
    }

    public PageResponse<OrderSummaryResponse> myOrders(Long actorId, int page, int size) {
        return myOrders(actorId, page, size, null);
    }

    @Transactional(readOnly = true)
    public OrderResponse myDetail(Long id, Long actorId) {
        var customer = requireCustomer(actorId);
        var order = orders.findByOrderIdAndCustomer_CustomerId(id, customer.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return response(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> staffList(int page, int size, OrderStatus status,
            LocalDateTime from, LocalDateTime to) {
        return staffList(page, size, null, status, from, to);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> staffList(int page, int size, String keyword,
            OrderStatus status, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from phải trước hoặc bằng to");
        }
        Specification<Order> filter = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), to));
            if (keyword != null && !keyword.isBlank()) {
                String term = "%" + keyword.strip().toLowerCase(java.util.Locale.ROOT) + "%";
                var customer = root.join("customer");
                var match = builder.or(
                        builder.like(builder.lower(customer.get("customerName")), term),
                        builder.like(builder.lower(customer.get("phone")), term));
                try {
                    match = builder.or(match, builder.equal(root.get("orderId"), Long.valueOf(keyword.strip().replace("#", ""))));
                } catch (NumberFormatException ignored) {
                    // The keyword is a customer name or phone.
                }
                predicates.add(match);
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return InventoryPagination.response(orders.findAll(filter, InventoryPagination.page(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "orderId"))), OrderMapper::summary);
    }

    @Transactional(readOnly = true)
    public OrderResponse staffDetail(Long id) {
        return response(orders.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng")));
    }

    @Transactional
    public OrderResponse customerCancel(Long id, Long actorId) {
        var customer = requireCustomer(actorId);
        var order = orders.findOwnedByIdForUpdate(id, customer.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return cancelLocked(order, actorId, true);
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderStatus> allowedStaffTransitions(OrderStatus status) {
        return OrderStatePolicy.allowedStaffTransitions(status);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus target, Long actorId) {
        var order = orders.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (target == OrderStatus.CANCELLED) {
            return cancelLocked(order, actorId, false);
        }
        OrderStatePolicy.requireForwardTransition(order.getStatus(), target);
        order.setStatus(target);
        orders.saveAndFlush(order);
        return response(order);
    }

    private OrderResponse cancelLocked(Order order, Long actorId, boolean customerAction) {
        OrderStatePolicy.requireCancellable(order.getStatus(), customerAction);
        var items = details.findItemsForCancellation(order.getOrderId());
        if (items.isEmpty()) {
            throw new ResourceConflictException("Đơn hàng không có dòng hàng để hoàn kho");
        }
        var actor = users.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        inventory.restoreForCancellation(order.getOrderId(), items, actor);
        vouchers.restore(order.getVoucher() == null ? null : order.getVoucher().getVoucherId());
        order.setStatus(OrderStatus.CANCELLED);
        orders.saveAndFlush(order);
        return response(order);
    }

    private Customer requireCustomer(Long actorId) {
        return customers.findByUser_UserId(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ Customer"));
    }

    private OrderResponse response(Order order) {
        return OrderMapper.order(order, details.findById_OrderIdOrderById_VariantIdAsc(order.getOrderId()));
    }
}
