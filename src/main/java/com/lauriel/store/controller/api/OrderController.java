package com.lauriel.store.controller.api;

import com.lauriel.store.dto.request.CheckoutRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.entity.enums.SalesChannel;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;
    private final com.lauriel.store.service.VoucherService vouchers;

    public OrderController(
        OrderService orders,
        com.lauriel.store.service.VoucherService vouchers
    ) {
        this.orders = orders;
        this.vouchers = vouchers;
    }

    @PostMapping("/preview")
    public VoucherValidationResponse preview(
        @Valid @RequestBody com.lauriel.store.dto.request.OrderPreviewRequest request
    ) {
        return vouchers.preview(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
        @Valid @RequestBody CheckoutRequest request,
        @AuthenticationPrincipal StoreUserDetails user
    ) {
        return orders.checkout(request, user.getUserId(), SalesChannel.MOBILE);
    }

    @GetMapping("/my-orders")
    public PageResponse<OrderSummaryResponse> myOrders(
        @AuthenticationPrincipal StoreUserDetails user,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
        @RequestParam(required = false) OrderStatus status
    ) {
        return orders.myOrders(user.getUserId(), page, size, status);
    }

    @GetMapping("/{id}")
    public OrderResponse detail(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal StoreUserDetails user
    ) {
        return orders.myDetail(id, user.getUserId());
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal StoreUserDetails user
    ) {
        return orders.customerCancel(id, user.getUserId());
    }
}
