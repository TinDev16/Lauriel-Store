package com.lauriel.store.controller.api.admin;

import com.lauriel.store.dto.request.ProductVariantRequest;
import com.lauriel.store.dto.response.ProductVariantResponse;
import com.lauriel.store.service.ProductVariantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/product-variants")
public class AdminProductVariantController {

    private final ProductVariantService service;

    public AdminProductVariantController(ProductVariantService service) {
        this.service = service;
    }

    @PutMapping("/{variantId}")
    public ProductVariantResponse update(
        @PathVariable @Positive Long variantId,
        @Valid @RequestBody ProductVariantRequest request
    ) {
        return service.update(variantId, request);
    }
}
