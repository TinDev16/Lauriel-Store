package com.lauriel.store.controller.api.admin;

import com.lauriel.store.dto.request.PromotionRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.service.PromotionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/promotions")
public class AdminPromotionController {

    private final PromotionService promotions;

    public AdminPromotionController(PromotionService promotions) {
        this.promotions = promotions;
    }

    @GetMapping
    public PageResponse<PromotionResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return promotions.list(page, size, false);
    }

    @GetMapping("/{id}")
    public PromotionResponse detail(@PathVariable @Positive Long id) {
        return promotions.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionResponse create(
        @Valid @RequestBody PromotionRequest request
    ) {
        return promotions.create(request);
    }

    @PutMapping("/{id}")
    public PromotionResponse update(
        @PathVariable @Positive Long id,
        @Valid @RequestBody PromotionRequest request
    ) {
        return promotions.update(id, request);
    }

    @GetMapping("/{id}/products")
    public PageResponse<ProductSummaryResponse> products(
        @PathVariable @Positive Long id,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return promotions.products(id, page, size);
    }

    @PostMapping("/{id}/products/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductSummaryResponse addProduct(
        @PathVariable @Positive Long id,
        @PathVariable @Positive Long productId
    ) {
        return promotions.addProduct(id, productId);
    }

    @DeleteMapping("/{id}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProduct(
        @PathVariable @Positive Long id,
        @PathVariable @Positive Long productId
    ) {
        promotions.removeProduct(id, productId);
    }
}
