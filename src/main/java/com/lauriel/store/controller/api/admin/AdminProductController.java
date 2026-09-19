package com.lauriel.store.controller.api.admin;

import com.lauriel.store.dto.request.ProductRequest;
import com.lauriel.store.dto.request.ProductStatusRequest;
import com.lauriel.store.dto.request.ProductVariantRequest;
import com.lauriel.store.dto.response.ProductResponse;
import com.lauriel.store.dto.response.ProductVariantResponse;
import com.lauriel.store.service.ProductService;
import com.lauriel.store.service.ProductVariantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductService products;
    private final ProductVariantService variants;

    public AdminProductController(ProductService products, ProductVariantService variants) {
        this.products = products;
        this.variants = variants;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return products.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable @Positive Long id, @Valid @RequestBody ProductRequest request) {
        return products.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductResponse status(@PathVariable @Positive Long id,
            @Valid @RequestBody ProductStatusRequest request) {
        return products.updateStatus(id, request.status());
    }

    @PostMapping("/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductVariantResponse createVariant(@PathVariable @Positive Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        return variants.create(productId, request);
    }
}
