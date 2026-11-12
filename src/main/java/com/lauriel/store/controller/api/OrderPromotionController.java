package com.lauriel.store.controller.api;

import com.lauriel.store.dto.request.OrderPreviewRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.service.OrderPromotionService;
import com.lauriel.store.service.PricingService;
import com.lauriel.store.service.VoucherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order-promotions")
public class OrderPromotionController {

    private final OrderPromotionService service;
    private final PricingService pricing;
    private final VoucherService vouchers;

    public OrderPromotionController(
        OrderPromotionService service,
        PricingService pricing,
        VoucherService vouchers
    ) {
        this.service = service;
        this.pricing = pricing;
        this.vouchers = vouchers;
    }

    @GetMapping("/active")
    public PageResponse<OrderPromotionResponse> active(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return service.active(page, size);
    }

    @GetMapping("/suggestion")
    public ResponseEntity<OrderPromotionSuggestion> suggestion(
        @RequestParam(required = false) BigDecimal amount
    ) {
        return ResponseEntity.ok(pricing.nextOrderPromotionSuggestion(amount));
    }

    @PostMapping("/suggestion")
    public ResponseEntity<OrderPromotionSuggestion> suggestionFromItems(
        @Valid @RequestBody OrderPreviewRequest request
    ) {
        var preview = vouchers.preview(request);
        return ResponseEntity.ok(preview.orderPromotionSuggestion());
    }
}
