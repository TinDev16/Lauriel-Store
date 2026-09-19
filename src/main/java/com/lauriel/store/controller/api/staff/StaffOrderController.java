package com.lauriel.store.controller.api.staff;

import com.lauriel.store.dto.request.OrderStatusRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/orders")
public class StaffOrderController {
    private final OrderService orders;

    public StaffOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return orders.staffList(page, size, status, from, to);
    }

    @GetMapping("/{id}")
    public OrderResponse detail(@PathVariable @Positive Long id) {
        return orders.staffDetail(id);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse status(@PathVariable @Positive Long id, @Valid @RequestBody OrderStatusRequest request,
            @AuthenticationPrincipal StoreUserDetails user) {
        return orders.updateStatus(id, request.status(), user.getUserId());
    }
}
