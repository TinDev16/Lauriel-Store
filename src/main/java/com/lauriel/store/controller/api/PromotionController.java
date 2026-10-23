package com.lauriel.store.controller.api;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.service.PromotionService;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotions;

    public PromotionController(PromotionService promotions) {
        this.promotions = promotions;
    }

    @GetMapping("/active")
    public PageResponse<PromotionResponse> active(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return promotions.list(page, size, true);
    }
}
