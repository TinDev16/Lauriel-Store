package com.lauriel.store.service;

import com.lauriel.store.dto.request.PromotionRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.exception.*;
import com.lauriel.store.mapper.*;
import com.lauriel.store.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionService {
    private final PromotionRepository promotions;
    private final PromotionProductRepository associations;
    private final ProductRepository products;
    private final PricingService pricing;

    public PromotionService(PromotionRepository promotions, PromotionProductRepository associations,
            ProductRepository products, PricingService pricing) {
        this.promotions = promotions;
        this.associations = associations;
        this.products = products;
        this.pricing = pricing;
    }

    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> list(int page, int size, boolean activeOnly) {
        var pageable = InventoryPagination.page(page, size, Sort.by("promotionId"));
        var now = pricing.now();
        var result = activeOnly
                ? promotions.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        ActiveStatus.ACTIVE, now, now, pageable)
                : promotions.findAll(pageable);
        return InventoryPagination.response(result, MarketingMapper::promotion);
    }

    @Transactional(readOnly = true)
    public PromotionResponse detail(Long id) {
        return MarketingMapper.promotion(promotions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Promotion")));
    }

    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        var promotion = new Promotion();
        apply(promotion, request);
        return MarketingMapper.promotion(promotions.saveAndFlush(promotion));
    }

    @Transactional
    public PromotionResponse update(Long id, PromotionRequest request) {
        var promotion = lock(id);
        apply(promotion, request);
        return MarketingMapper.promotion(promotions.saveAndFlush(promotion));
    }

    @Transactional
    public PromotionResponse updateStatus(Long id,ActiveStatus status) {
        var promotion=lock(id); promotion.setStatus(status);
        return MarketingMapper.promotion(promotions.saveAndFlush(promotion));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> products(Long id, int page, int size) {
        if (!promotions.existsById(id)) throw new ResourceNotFoundException("Không tìm thấy Promotion");
        return InventoryPagination.response(associations.findById_PromotionId(id,
                InventoryPagination.page(page, size, Sort.by("id.productId"))), association -> {
            var product = association.getProduct();
            return new ProductSummaryResponse(product.getProductId(), product.getProductName(), product.getPrice(),
                    product.getImgUrl(), product.getStatus());
        });
    }

    @Transactional
    public ProductSummaryResponse addProduct(Long id, Long productId) {
        var promotion = lock(id);
        var product = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        var key = new PromotionProductId(id, productId);
        if (associations.existsById(key)) throw new DuplicateResourceException("Sản phẩm đã thuộc Promotion");
        var association = new PromotionProduct();
        association.setId(key);
        association.setPromotion(promotion);
        association.setProduct(product);
        associations.saveAndFlush(association);
        return new ProductSummaryResponse(productId, product.getProductName(), product.getPrice(),
                product.getImgUrl(), product.getStatus());
    }

    @Transactional
    public void removeProduct(Long id, Long productId) {
        lock(id);
        var association = associations.findById(new PromotionProductId(id, productId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy liên kết Promotion/Product"));
        associations.delete(association);
        associations.flush();
    }

    private Promotion lock(Long id) {
        return promotions.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Promotion"));
    }

    private void apply(Promotion promotion, PromotionRequest request) {
        promotion.setPromotionName(request.promotionName().strip());
        promotion.setDescription(request.description());
        promotion.setDiscountPercent(request.discountPercent());
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        promotion.setStatus(request.status());
    }
}
