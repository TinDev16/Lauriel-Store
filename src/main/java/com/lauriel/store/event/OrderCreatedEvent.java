package com.lauriel.store.event;

import com.lauriel.store.entity.Order;
import com.lauriel.store.entity.OrderDetail;
import com.lauriel.store.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        String customerName,
        String phone,
        String shippingAddress,
        BigDecimal totalAmount,
        String orderPromotionName,
        BigDecimal orderPromotionDiscount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal finalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        List<Item> items) {

    public OrderCreatedEvent {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public OrderCreatedEvent(Long orderId, String customerName, String phone, String shippingAddress,
            BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal shippingFee, BigDecimal finalAmount,
            OrderStatus status, LocalDateTime createdAt, List<Item> items) {
        this(orderId, customerName, phone, shippingAddress, totalAmount, null, BigDecimal.ZERO,
                discountAmount, shippingFee, finalAmount, status, createdAt, items);
    }

    public static OrderCreatedEvent snapshot(Order order, List<OrderDetail> details) {
        var customer = order.getCustomer();
        var itemSnapshots = details.stream().map(detail -> {
            var variant = detail.getProductVariant();
            return new Item(
                    variant.getProduct().getProductName(),
                    variant.getSize().getSizeName(),
                    variant.getColor().getColorName(),
                    detail.getQuantity());
        }).toList();
        return new OrderCreatedEvent(
                order.getOrderId(),
                customer == null ? null : customer.getCustomerName(),
                customer == null ? null : customer.getPhone(),
                order.getShippingAddress(),
                order.getTotalAmount(),
                order.getOrderPromotionName(),
                order.getOrderPromotionDiscount(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getFinalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                itemSnapshots);
    }

    public record Item(String productName, String size, String color, Integer quantity) {}
}
