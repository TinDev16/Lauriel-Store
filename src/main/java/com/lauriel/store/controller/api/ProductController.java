package com.lauriel.store.controller.api;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.service.ProductService;
import com.lauriel.store.service.ProductVariantService;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService products;
    private final ProductVariantService variants;

    public ProductController(
        ProductService products,
        ProductVariantService variants
    ) {
        this.products = products;
        this.variants = variants;
    }

    @GetMapping
    public PageResponse<ProductSummaryResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) @Positive Long categoryId,
        @RequestParam(required = false) @Positive Long sizeId,
        @RequestParam(required = false) @Positive Long colorId,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        return products.listPublic(
            page,
            size,
            keyword,
            categoryId,
            sizeId,
            colorId,
            sortBy,
            direction
        );
    }

    @GetMapping("/featured")
    public List<ProductSummaryResponse> featured(
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int limit
    ) {
        return products.featured(limit);
    }

    @GetMapping("/{id}")
    public ProductDetailResponse detail(@PathVariable @Positive Long id) {
        return products.getPublicDetail(id);
    }

    @GetMapping("/{id}/related")
    public List<ProductSummaryResponse> related(
        @PathVariable @Positive Long id,
        @RequestParam(defaultValue = "8") @Min(1) @Max(10) int limit
    ) {
        return products.related(id, limit);
    }

    @GetMapping("/{productId}/variants")
    public List<ProductVariantResponse> variants(
        @PathVariable @Positive Long productId
    ) {
        return variants.listPublic(productId);
    }
}
