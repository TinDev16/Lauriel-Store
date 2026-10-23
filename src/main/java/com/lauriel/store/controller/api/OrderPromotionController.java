package com.lauriel.store.controller.api;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.service.OrderPromotionService;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order-promotions")
public class OrderPromotionController {

    private final OrderPromotionService service;

    public OrderPromotionController(OrderPromotionService service) {
        this.service = service;
    }

    @GetMapping("/active")
    public PageResponse<OrderPromotionResponse> active(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return service.active(page, size);
    }
}
