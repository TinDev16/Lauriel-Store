package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;

class OrderStatePolicyTest {

    @Test
    void pendingAllowsAllForwardTransitionsAndCancellation() {
        var transitions = OrderStatePolicy.allowedStaffTransitions(OrderStatus.PENDING);
        assertThat(transitions).containsExactly(
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED
        );
        // All forward jumps are valid
        OrderStatePolicy.requireForwardTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        OrderStatePolicy.requireForwardTransition(OrderStatus.PENDING, OrderStatus.PROCESSING);
        OrderStatePolicy.requireForwardTransition(OrderStatus.PENDING, OrderStatus.SHIPPING);
        OrderStatePolicy.requireForwardTransition(OrderStatus.PENDING, OrderStatus.DELIVERED);

        // Cannot update backwards or to same or invalid
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.PENDING, OrderStatus.PENDING))
            .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.PENDING, OrderStatus.RETURNED))
            .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void confirmedAllowsForwardTransitionsAndNoBackwards() {
        var transitions = OrderStatePolicy.allowedStaffTransitions(OrderStatus.CONFIRMED);
        assertThat(transitions).containsExactly(
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED
        );
        // Cannot go backwards to PENDING
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.CONFIRMED, OrderStatus.PENDING))
            .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.CONFIRMED, OrderStatus.CONFIRMED))
            .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void processingAllowsForwardTransitionsAndNoBackwards() {
        var transitions = OrderStatePolicy.allowedStaffTransitions(OrderStatus.PROCESSING);
        assertThat(transitions).containsExactly(
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED
        );
        // Cannot go backwards
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.PROCESSING, OrderStatus.CONFIRMED))
            .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.PROCESSING, OrderStatus.PENDING))
            .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void shippingAllowsDeliveredAndReturned() {
        var transitions = OrderStatePolicy.allowedStaffTransitions(OrderStatus.SHIPPING);
        assertThat(transitions).containsExactly(
            OrderStatus.DELIVERED,
            OrderStatus.RETURNED
        );
        // Cannot go backwards
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.SHIPPING, OrderStatus.PROCESSING))
            .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.SHIPPING, OrderStatus.CONFIRMED))
            .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.SHIPPING, OrderStatus.PENDING))
            .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void deliveredAllowsReturnedOnly() {
        var transitions = OrderStatePolicy.allowedStaffTransitions(OrderStatus.DELIVERED);
        assertThat(transitions).containsExactly(
            OrderStatus.RETURNED
        );
        // Cannot go backwards to SHIPPING, PROCESSING, CONFIRMED, PENDING
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.DELIVERED, OrderStatus.SHIPPING))
            .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.DELIVERED, OrderStatus.PROCESSING))
            .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.DELIVERED, OrderStatus.CONFIRMED))
            .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.DELIVERED, OrderStatus.PENDING))
            .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void terminalStatesAllowNoTransitions() {
        assertThat(OrderStatePolicy.allowedStaffTransitions(OrderStatus.CANCELLED)).isEmpty();
        assertThat(OrderStatePolicy.allowedStaffTransitions(OrderStatus.RETURNED)).isEmpty();

        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED))
            .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> OrderStatePolicy.requireForwardTransition(OrderStatus.RETURNED, OrderStatus.DELIVERED))
            .isInstanceOf(ResourceConflictException.class);
    }
}

