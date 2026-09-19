package com.lauriel.store.service;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.Order;
import com.lauriel.store.entity.OrderDetail;
import com.lauriel.store.entity.OrderDetailId;
import com.lauriel.store.entity.Customer;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.exception.*;
import com.lauriel.store.mapper.OrderMapper;
import com.lauriel.store.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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

    public OrderService(OrderRepository orders, OrderDetailRepository details, CustomerRepository customers,
            UserRepository users, InventoryService inventory, PricingService pricing, VoucherService vouchers) {
        this.orders = orders;
        this.details = details;
        this.customers = customers;
        this.users = users;
        this.inventory = inventory;
        this.pricing = pricing;
        this.vouchers = vouchers;
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
        var applied = vouchers.consume(request.voucherCode(), total, now);
        order.setVoucher(applied.voucher());
        order.setTotalAmount(total);
        order.setDiscountAmount(applied.discount());
        order.setFinalAmount(total.subtract(applied.discount()));
        orders.saveAndFlush(order);
        for (var item : lines) {
            item.setId(new OrderDetailId(order.getOrderId(), item.getProductVariant().getVariantId()));
            details.saveAndFlush(item);
        }
        inventory.decreaseForSale(order.getOrderId(), lines, customer.getUser());
        return response(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> myOrders(Long actorId, int page, int size) {
        var customer = requireCustomer(actorId);
        return InventoryPagination.response(orders.findByCustomer_CustomerId(customer.getCustomerId(),
                InventoryPagination.page(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "orderId"))),
                OrderMapper::summary);
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
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from phải trước hoặc bằng to");
        }
        Specification<Order> filter = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), to));
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
