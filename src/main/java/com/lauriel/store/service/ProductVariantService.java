package com.lauriel.store.service;

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
    public ProductVariantResponse update(Long variantId, ProductVariantRequest request) {
        var variant = variants.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể"));
        apply(variant, request);
        return pricing.variantResponse(variants.saveAndFlush(variant));
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long,Long> counts(java.util.Collection<Long> ids) {
        var result = new java.util.HashMap<Long,Long>();
        if (!ids.isEmpty()) variants.countForProducts(ids).forEach(c -> result.put(c.getProductId(),c.getVariants()));
        return result;
    }
    @Transactional(readOnly = true)
    public com.lauriel.store.dto.response.PageResponse<ProductVariantResponse> listAdmin(Long productId,int page,int size) {
        if (!products.existsById(productId)) throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
        var result = variants.findByProduct_ProductId(productId, InventoryPagination.page(page,size,
                org.springframework.data.domain.Sort.by("variantId")));
        var prices=pricing.priceVariants(result.getContent(),pricing.now());
        return InventoryPagination.response(result, v -> {
            var p=prices.get(v.getVariantId());
            return new ProductVariantResponse(v.getVariantId(),productId,v.getSku(),CatalogMapper.size(v.getSize()),
                    CatalogMapper.color(v.getColor()),v.getPrice(),v.getQuantity(),v.getStatus(),p.effectivePrice(),p.promotion());
        });
    }
    @Transactional(readOnly = true)
    public ProductVariantResponse detail(Long productId,Long variantId) {
        var variant=variants.findById(variantId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể"));
        if (!variant.getProduct().getProductId().equals(productId)) throw new ResourceNotFoundException("Không tìm thấy biến thể");
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
        String sku = request.sku().strip();
        boolean duplicateSku;
        boolean duplicateAttributes;
        if (variant.getVariantId() == null) {
            duplicateSku = variants.existsBySkuIgnoreCase(sku);
            duplicateAttributes = variants.existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorId(
                    variant.getProduct().getProductId(), request.sizeId(), request.colorId());
        } else {
            duplicateSku = variants.existsBySkuIgnoreCaseAndVariantIdNot(sku, variant.getVariantId());
            duplicateAttributes = variants.existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorIdAndVariantIdNot(
                    variant.getProduct().getProductId(), request.sizeId(), request.colorId(), variant.getVariantId());
        }
        if (duplicateSku) {
            throw new DuplicateResourceException("SKU đã tồn tại");
        }
        if (duplicateAttributes) {
            throw new DuplicateResourceException("Biến thể với size và màu này đã tồn tại.");
        }
        variant.setSize(size);
        variant.setColor(color);
        variant.setSku(sku);
        variant.setPrice(request.price());
        variant.setStatus(request.status());
    }
}
