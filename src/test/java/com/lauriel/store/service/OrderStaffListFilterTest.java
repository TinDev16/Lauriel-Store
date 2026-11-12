package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lauriel.store.config.StoreCommerceProperties;
import com.lauriel.store.entity.Customer;
import com.lauriel.store.entity.Order;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class OrderStaffListFilterTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderDetailRepository orderDetailRepository = mock(OrderDetailRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final PricingService pricing = mock(PricingService.class);
    private final VoucherService vouchers = mock(VoucherService.class);
    private final StoreCommerceProperties commerce = mock(StoreCommerceProperties.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

    private final OrderService orderService = new OrderService(
        orderRepository,
        orderDetailRepository,
        customerRepository,
        userRepository,
        inventory,
        pricing,
        vouchers,
        commerce,
        events
    );

    @Test
    void staffListAcceptsCustomerIdAndQueriesRepository() {
        var customer = new Customer();
        customer.setCustomerId(99L);
        customer.setCustomerName("Test Customer");

        var order = new Order();
        order.setOrderId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DELIVERED);
        order.setTotalAmount(new BigDecimal("500000.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(new BigDecimal("500000.00"));
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(order)));

        var result = orderService.staffList(
            0,
            10,
            null,
            OrderStatus.DELIVERED,
            null,
            null,
            99L
        );

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).orderId()).isEqualTo(1L);
        assertThat(result.content().get(0).customerId()).isEqualTo(99L);

        verify(orderRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
