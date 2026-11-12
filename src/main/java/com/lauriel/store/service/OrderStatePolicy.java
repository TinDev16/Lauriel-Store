package com.lauriel.store.service;

import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.exception.ResourceConflictException;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class OrderStatePolicy {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_FORWARD = Map.of(
        OrderStatus.PENDING, Set.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED
        ),
        OrderStatus.CONFIRMED, Set.of(
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED
        ),
        OrderStatus.PROCESSING, Set.of(
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED
        ),
        OrderStatus.SHIPPING, Set.of(
            OrderStatus.DELIVERED,
            OrderStatus.RETURNED
        ),
        OrderStatus.DELIVERED, Set.of(
            OrderStatus.RETURNED
        )
    );

    private static final List<OrderStatus> PROGRESSION_ORDER = List.of(
        OrderStatus.CONFIRMED,
        OrderStatus.PROCESSING,
        OrderStatus.SHIPPING,
        OrderStatus.DELIVERED,
        OrderStatus.RETURNED
    );

    private static final Set<OrderStatus> CUSTOMER_CANCELLABLE = Set.of(
        OrderStatus.PENDING,
        OrderStatus.CONFIRMED
    );

    private static final Set<OrderStatus> STAFF_CANCELLABLE = Set.of(
        OrderStatus.PENDING,
        OrderStatus.CONFIRMED,
        OrderStatus.PROCESSING
    );

    private OrderStatePolicy() {}

    static List<OrderStatus> allowedStaffTransitions(
        OrderStatus current
    ) {
        if (current == null) {
            return List.of();
        }
        var targets = new java.util.ArrayList<OrderStatus>();
        var forward = ALLOWED_FORWARD.get(current);
        if (forward != null) {
            for (OrderStatus s : PROGRESSION_ORDER) {
                if (forward.contains(s)) {
                    targets.add(s);
                }
            }
        }
        if (STAFF_CANCELLABLE.contains(current)) {
            targets.add(OrderStatus.CANCELLED);
        }
        return List.copyOf(targets);
    }

    static void requireForwardTransition(
        OrderStatus current,
        OrderStatus target
    ) {
        if (target == null) {
            throw new ResourceConflictException(
                "Chuyển trạng thái đơn hàng không hợp lệ"
            );
        }
        var forward = ALLOWED_FORWARD.get(current);
        if (forward == null || !forward.contains(target)) {
            throw new ResourceConflictException(
                "Chuyển trạng thái đơn hàng không hợp lệ"
            );
        }
    }

    static void requireCancellable(
        OrderStatus current,
        boolean customerAction
    ) {
        if (
            !(
                customerAction ? CUSTOMER_CANCELLABLE : STAFF_CANCELLABLE
            ).contains(current)
        ) {
            throw new ResourceConflictException(
                "Đơn hàng không thể hủy ở trạng thái hiện tại"
            );
        }
    }
}
