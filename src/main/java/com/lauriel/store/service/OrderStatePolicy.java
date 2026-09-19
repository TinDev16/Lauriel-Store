package com.lauriel.store.service;

import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.exception.ResourceConflictException;
import java.util.Map;
import java.util.Set;

final class OrderStatePolicy {
    private static final Map<OrderStatus, OrderStatus> FORWARD = Map.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED,
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
            OrderStatus.PROCESSING, OrderStatus.SHIPPING,
            OrderStatus.SHIPPING, OrderStatus.DELIVERED);
    private static final Set<OrderStatus> CUSTOMER_CANCELLABLE =
            Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);
    private static final Set<OrderStatus> STAFF_CANCELLABLE =
            Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);

    static java.util.List<OrderStatus> allowedStaffTransitions(OrderStatus current) {
        var targets=new java.util.ArrayList<OrderStatus>();
        if (FORWARD.containsKey(current)) targets.add(FORWARD.get(current));
        if (STAFF_CANCELLABLE.contains(current)) targets.add(OrderStatus.CANCELLED);
        return java.util.List.copyOf(targets);
    }

    private OrderStatePolicy() {}

    static void requireForwardTransition(OrderStatus current, OrderStatus target) {
        if (target == null || FORWARD.get(current) != target) {
            throw new ResourceConflictException("Chuyển trạng thái đơn hàng không hợp lệ");
        }
    }

    static void requireCancellable(OrderStatus current, boolean customerAction) {
        if (!(customerAction ? CUSTOMER_CANCELLABLE : STAFF_CANCELLABLE).contains(current)) {
            throw new ResourceConflictException("Đơn hàng không thể hủy ở trạng thái hiện tại");
        }
    }
}
