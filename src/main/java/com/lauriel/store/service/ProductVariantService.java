package com.lauriel.store.service;

import com.lauriel.store.dto.request.BulkProductVariantRequest;
import com.lauriel.store.dto.request.ProductVariantRequest;
import com.lauriel.store.dto.response.ProductVariantResponse;
import com.lauriel.store.entity.ProductVariant;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.exception.DuplicateResourceException;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.mapper.CatalogMapper;
import com.lauriel.store.repository.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductVariantService {
    public record BulkCreateResult(int created, int skipped) {}
    public record ProductMetrics(java.util.Map<Long, Long> attributeCounts,
            java.util.Map<Long, Long> stockTotals) {}
    private final ProductVariantRepository variants;
    private final ProductRepository products;
    private final SizeRepository sizes;
    private final ColorRepository colors;
    private final PricingService pricing;

    public ProductVariantService(ProductVariantRepository variants, ProductRepository products,
            SizeRepository sizes, ColorRepository colors, PricingService pricing) {
        this.variants = variants;
        this.products = products;
        this.sizes = sizes;
        this.colors = colors;
        this.pricing = pricing;
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> listPublic(Long productId) {
        if (!products.existsByProductIdAndStatus(productId, ActiveStatus.ACTIVE)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
        }
        return pricing.publicVariants(productId);
    }

    @Transactional
    public ProductVariantResponse create(Long productId, ProductVariantRequest request) {
        var product = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        // Stock starts at zero. Inventory operations will own quantity from Phase 4.
        variant.setQuantity(0);
        apply(variant, request);
        return pricing.variantResponse(variants.saveAndFlush(variant));
    }


    @Transactional
    public BulkCreateResult createBulk(Long productId, BulkProductVariantRequest request) {
        var product = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        var selectedSizeIds = new java.util.LinkedHashSet<>(request.sizeIds());
        var selectedColorIds = new java.util.LinkedHashSet<>(request.colorIds());

        var sizeById = sizes.findAllById(selectedSizeIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.lauriel.store.entity.Size::getSizeId, java.util.function.Function.identity()));
        var colorById = colors.findAllById(selectedColorIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.lauriel.store.entity.Color::getColorId, java.util.function.Function.identity()));
        if (sizeById.size() != selectedSizeIds.size()) {
            throw new ResourceNotFoundException("Có kích cỡ không còn tồn tại");
        }
        if (colorById.size() != selectedColorIds.size()) {
            throw new ResourceNotFoundException("Có màu sắc không còn tồn tại");
        }

        var existing = variants.findAttributesByProductId(productId).stream()
                .map(item -> item.getSizeId() + ":" + item.getColorId())
                .collect(java.util.stream.Collectors.toSet());
        var created = new java.util.ArrayList<ProductVariant>();
        int skipped = 0;
        for (Long sizeId : selectedSizeIds) {
            for (Long colorId : selectedColorIds) {
                if (existing.contains(sizeId + ":" + colorId)) {
                    skipped++;
                    continue;
                }
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setSize(sizeById.get(sizeId));
                variant.setColor(colorById.get(colorId));
                variant.setSku(nextSku(productId, sizeId, colorId));
                variant.setPrice(request.price());
                variant.setQuantity(0);
                variant.setStatus(request.status());
                created.add(variant);
            }
        }
        if (created.isEmpty()) {
            throw new DuplicateResourceException("Tất cả tổ hợp đã tồn tại.");
        }
        variants.saveAllAndFlush(created);
        return new BulkCreateResult(created.size(), skipped);
    }
    @Transactional
    public ProductVariantResponse update(Long variantId, ProductVariantRequest request) {
        var variant = variants.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính"));
        apply(variant, request);
        return pricing.variantResponse(variants.saveAndFlush(variant));
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long,Long> counts(java.util.Collection<Long> ids) {
        return metrics(ids).attributeCounts();
    }
    @Transactional(readOnly = true)
    public ProductMetrics metrics(java.util.Collection<Long> ids) {
        var counts = new java.util.HashMap<Long, Long>();
        var stocks = new java.util.HashMap<Long, Long>();
        if (!ids.isEmpty()) {
            variants.countForProducts(ids).forEach(item -> {
                counts.put(item.getProductId(), item.getVariants());
                stocks.put(item.getProductId(), item.getTotalQuantity());
            });
        }
        return new ProductMetrics(counts, stocks);
    }
    @Transactional(readOnly = true)
    public com.lauriel.store.dto.response.PageResponse<ProductVariantResponse> listAdmin(Long productId,int page,int size) {
        return listAdmin(productId, page, size, null, null, null);
    }
    @Transactional(readOnly = true)
    public com.lauriel.store.dto.response.PageResponse<ProductVariantResponse> listAdmin(Long productId,int page,int size,
            String keyword, ActiveStatus status, Long colorId) {
        if (!products.existsById(productId)) throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
        org.springframework.data.jpa.domain.Specification<ProductVariant> filter = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.equal(root.get("product").get("productId"), productId));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (colorId != null) predicates.add(builder.equal(root.get("color").get("colorId"), colorId));
            if (keyword != null && !keyword.isBlank()) {
                String term = "%" + keyword.strip().toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("sku")), term),
                        builder.like(builder.lower(root.get("size").get("sizeName")), term),
                        builder.like(builder.lower(root.get("color").get("colorName")), term)));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = variants.findAll(filter, InventoryPagination.page(page, size,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.asc("color.colorName"),
                        org.springframework.data.domain.Sort.Order.asc("color.colorId"),
                        org.springframework.data.domain.Sort.Order.asc("status"),
                        org.springframework.data.domain.Sort.Order.asc("size.sizeName"),
                        org.springframework.data.domain.Sort.Order.asc("size.sizeId"),
                        org.springframework.data.domain.Sort.Order.asc("variantId"))));
        var prices=pricing.priceVariants(result.getContent(),pricing.now());
        return InventoryPagination.response(result, v -> {
            var p=prices.get(v.getVariantId());
            return new ProductVariantResponse(v.getVariantId(),productId,v.getSku(),CatalogMapper.size(v.getSize()),
                    CatalogMapper.color(v.getColor()),v.getPrice(),v.getQuantity(),v.getStatus(),p.effectivePrice(),p.promotion());
        });
    }
    @Transactional(readOnly = true)
    public ProductVariantResponse detail(Long productId,Long variantId) {
        var variant=variants.findById(variantId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính"));
        if (!variant.getProduct().getProductId().equals(productId)) throw new ResourceNotFoundException("Không tìm thấy thuộc tính");
        return pricing.variantResponse(variant);
    }
    @Transactional
    public ProductVariantResponse updateForProduct(Long productId,Long variantId,ProductVariantRequest request) {
        detail(productId,variantId);
        return update(variantId,request);
    }
    private void apply(ProductVariant variant, ProductVariantRequest request) {
        var size = sizes.findById(request.sizeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy size"));
        var color = colors.findById(request.colorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy màu"));
        boolean creating = variant.getVariantId() == null;
        boolean duplicateAttributes;
        if (creating) {
            duplicateAttributes = variants.existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorId(
                    variant.getProduct().getProductId(), request.sizeId(), request.colorId());
        } else {
            duplicateAttributes = variants.existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorIdAndVariantIdNot(
                    variant.getProduct().getProductId(), request.sizeId(), request.colorId(), variant.getVariantId());
        }
        if (duplicateAttributes) {
            throw new DuplicateResourceException("Thuộc tính với size và màu này đã tồn tại.");
        }
        variant.setSize(size);
        variant.setColor(color);
        if (creating) variant.setSku(nextSku(variant.getProduct().getProductId(), request.sizeId(), request.colorId()));
        variant.setPrice(request.price());
        variant.setStatus(request.status());
    }

    private String nextSku(Long productId, Long sizeId, Long colorId) {
        String base = "SP-%d-S%d-C%d".formatted(productId, sizeId, colorId);
        String candidate = base;
        int suffix = 2;
        while (variants.existsBySkuIgnoreCase(candidate)) candidate = base + "-" + suffix++;
        return candidate;
    }
}
