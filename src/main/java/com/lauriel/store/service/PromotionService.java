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
    private final ProductVariantRepository variants;
    private final PromotionVariantRepository variantAssociations;
    private final PricingService pricing;

    public PromotionService(
        PromotionRepository promotions,
        PromotionProductRepository associations,
        ProductRepository products,
        ProductVariantRepository variants,
        PromotionVariantRepository variantAssociations,
        PricingService pricing
    ) {
        this.promotions = promotions;
        this.associations = associations;
        this.products = products;
        this.variants = variants;
        this.variantAssociations = variantAssociations;
        this.pricing = pricing;
    }

    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> list(
        int page,
        int size,
        boolean activeOnly
    ) {
        return list(page, size, activeOnly, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> list(
        int page,
        int size,
        boolean activeOnly,
        String keyword,
        ActiveStatus status
    ) {
        return list(page, size, activeOnly, keyword, status, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> list(
        int page,
        int size,
        boolean activeOnly,
        String keyword,
        ActiveStatus status,
        String timeFilter
    ) {
        var pageable = InventoryPagination.page(
            page,
            size,
            Sort.by(Sort.Order.asc("status"), Sort.Order.asc("promotionId"))
        );
        var now = pricing.now();
        if (activeOnly) {
            return InventoryPagination.response(
                promotions.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    ActiveStatus.ACTIVE,
                    now,
                    now,
                    pageable
                ),
                MarketingMapper::promotion
            );
        }
        String term =
            keyword == null || keyword.isBlank()
                ? null
                : "%" +
                  keyword.strip().toLowerCase(java.util.Locale.ROOT) +
                  "%";
        org.springframework.data.jpa.domain.Specification<Promotion> filter = (
            root,
            query,
            builder
        ) -> {
            var predicates =
                new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) predicates.add(
                builder.equal(root.get("status"), status)
            );
            if ("RUNNING".equalsIgnoreCase(timeFilter)) {
                predicates.add(builder.equal(root.get("status"), ActiveStatus.ACTIVE));
                predicates.add(builder.lessThanOrEqualTo(root.get("startDate"), now));
                predicates.add(builder.greaterThanOrEqualTo(root.get("endDate"), now));
            } else if ("UPCOMING".equalsIgnoreCase(timeFilter)) {
                predicates.add(builder.equal(root.get("status"), ActiveStatus.ACTIVE));
                predicates.add(builder.greaterThan(root.get("startDate"), now));
            } else if ("EXPIRED".equalsIgnoreCase(timeFilter)) {
                predicates.add(builder.lessThan(root.get("endDate"), now));
            } else if ("INACTIVE".equalsIgnoreCase(timeFilter)) {
                predicates.add(builder.equal(root.get("status"), ActiveStatus.INACTIVE));
            }
            if (term != null) predicates.add(
                builder.or(
                    builder.like(
                        builder.lower(root.get("promotionName")),
                        term
                    ),
                    builder.like(builder.lower(root.get("description")), term)
                )
            );
            return builder.and(
                predicates.toArray(
                    jakarta.persistence.criteria.Predicate[]::new
                )
            );
        };
        return InventoryPagination.response(
            promotions.findAll(filter, pageable),
            MarketingMapper::promotion
        );
    }

    @Transactional(readOnly = true)
    public PromotionResponse detail(Long id) {
        return MarketingMapper.promotion(
            promotions
                .findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Không tìm thấy Promotion")
                )
        );
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
    public PromotionResponse updateStatus(Long id, ActiveStatus status) {
        var promotion = lock(id);
        promotion.setStatus(status);
        return MarketingMapper.promotion(promotions.saveAndFlush(promotion));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> products(
        Long id,
        int page,
        int size
    ) {
        return products(id, page, size, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> products(
        Long id,
        int page,
        int size,
        String keyword,
        ActiveStatus status
    ) {
        if (!promotions.existsById(id)) throw new ResourceNotFoundException(
            "Không tìm thấy Promotion"
        );
        String term =
            keyword == null || keyword.isBlank() ? null : keyword.strip();
        var linkedProducts = associations.findAdminProducts(
            id,
            term,
            status,
            InventoryPagination.page(page, size, Sort.by("id.productId"))
        );
        var basePrices = linkedProducts
            .getContent()
            .stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    association -> association.getProduct().getProductId(),
                    association -> association.getProduct().getPrice(),
                    (first, ignored) -> first,
                    java.util.LinkedHashMap::new
                )
            );
        var effectivePrices = pricing.priceProducts(basePrices, pricing.now());
        return InventoryPagination.response(linkedProducts, association -> {
            var product = association.getProduct();
            var price = effectivePrices.get(product.getProductId());
            return new ProductSummaryResponse(
                product.getProductId(),
                product.getProductName(),
                CatalogMapper.category(product.getCategory()),
                product.getPrice(),
                product.getImgUrl(),
                product.getStatus(),
                price.effectivePrice(),
                price.promotion()
            );
        });
    }

    @Transactional(readOnly = true)
    public java.util.List<ProductSummaryResponse> selectableProducts(
        Long id,
        String keyword,
        Long categoryId
    ) {
        if (!promotions.existsById(id)) throw new ResourceNotFoundException(
            "Không tìm thấy Promotion"
        );
        String term =
            keyword == null || keyword.isBlank() ? null : keyword.strip();
        return products
            .findPromotionCandidates(
                id,
                term,
                categoryId,
                org.springframework.data.domain.PageRequest.of(0, 200)
            )
            .stream()
            .map(CatalogMapper::productSummary)
            .toList();
    }

    @Transactional
    public ProductSummaryResponse addProduct(Long id, Long productId) {
        addProducts(id, java.util.List.of(productId));
        return CatalogMapper.productSummary(
            products
                .findById(productId)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Không tìm thấy sản phẩm")
                )
        );
    }

    @Transactional
    public int addProducts(
        Long id,
        java.util.Collection<Long> requestedProductIds
    ) {
        var promotion = lock(id);
        if (requestedProductIds == null) {
            throw new BadRequestException("Vui lòng chọn ít nhất một sản phẩm");
        }
        var ids = requestedProductIds
            .stream()
            .filter(java.util.Objects::nonNull)
            .collect(
                java.util.stream.Collectors.toCollection(
                    java.util.LinkedHashSet::new
                )
            );
        if (
            ids.isEmpty() ||
            ids.stream().anyMatch(productId -> productId <= 0) ||
            ids.size() > 200
        ) {
            throw new BadRequestException(
                "Vui lòng chọn từ 1 đến 200 sản phẩm hợp lệ"
            );
        }
        var found = products.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new ResourceNotFoundException(
                "Có sản phẩm đã chọn không còn tồn tại"
            );
        }
        var linked = new java.util.HashSet<>(
            associations.findLinkedProductIds(id, ids)
        );
        if (linked.size() == ids.size()) {
            throw new DuplicateResourceException(
                "Các sản phẩm đã chọn đều thuộc Promotion"
            );
        }
        var additions = found
            .stream()
            .filter(product -> !linked.contains(product.getProductId()))
            .map(product -> {
                var association = new PromotionProduct();
                association.setId(
                    new PromotionProductId(id, product.getProductId())
                );
                association.setPromotion(promotion);
                association.setProduct(product);
                return association;
            })
            .toList();
        associations.saveAll(additions);
        associations.flush();
        return additions.size();
    }

    public record VariantScope(
        PromotionResponse promotion,
        ProductSummaryResponse product,
        java.util.List<ProductVariantResponse> variants,
        java.util.List<SizeResponse> sizes,
        java.util.List<ColorResponse> colors,
        java.util.Set<Long> selectedVariantIds
    ) {
        public boolean allVariants() {
            return selectedVariantIds.isEmpty();
        }
    }

    @Transactional(readOnly = true)
    public VariantScope variantScope(Long id, Long productId) {
        var association = associations
            .findById(new PromotionProductId(id, productId))
            .orElseThrow(() ->
                new ResourceNotFoundException("Sản phẩm chưa thuộc khuyến mãi")
            );
        var found =
            variants.findByProduct_ProductIdOrderByColor_ColorNameAscSize_SizeNameAscVariantIdAsc(
                productId
            );
        var prices = pricing.priceVariants(found, pricing.now());
        var responses = found
            .stream()
            .map(variant -> {
                var price = prices.get(variant.getVariantId());
                return new ProductVariantResponse(
                    variant.getVariantId(),
                    productId,
                    variant.getSku(),
                    CatalogMapper.size(variant.getSize()),
                    CatalogMapper.color(variant.getColor()),
                    variant.getPrice(),
                    variant.getQuantity(),
                    variant.getStatus(),
                    price.effectivePrice(),
                    price.promotion()
                );
            })
            .toList();
        var scopeSizes = responses
            .stream()
            .map(ProductVariantResponse::size)
            .collect(
                java.util.stream.Collectors.toMap(
                    SizeResponse::sizeId,
                    value -> value,
                    (first, ignored) -> first,
                    java.util.LinkedHashMap::new
                )
            )
            .values()
            .stream()
            .toList();
        var scopeColors = responses
            .stream()
            .map(ProductVariantResponse::color)
            .collect(
                java.util.stream.Collectors.toMap(
                    ColorResponse::colorId,
                    value -> value,
                    (first, ignored) -> first,
                    java.util.LinkedHashMap::new
                )
            )
            .values()
            .stream()
            .toList();
        return new VariantScope(
            MarketingMapper.promotion(association.getPromotion()),
            CatalogMapper.productSummary(association.getProduct()),
            responses,
            scopeSizes,
            scopeColors,
            new java.util.LinkedHashSet<>(
                variantAssociations.findSelectedVariantIds(id, productId)
            )
        );
    }

    @Transactional
    public void configureVariantScope(
        Long id,
        Long productId,
        boolean allVariants,
        java.util.Collection<Long> requestedVariantIds
    ) {
        var promotion = lock(id);
        associations
            .findById(new PromotionProductId(id, productId))
            .orElseThrow(() ->
                new ResourceNotFoundException("Sản phẩm chưa thuộc khuyến mãi")
            );
        clearVariantScope(id, productId);
        variantAssociations.flush();
        if (allVariants) return;
        var ids =
            requestedVariantIds == null
                ? new java.util.LinkedHashSet<Long>()
                : requestedVariantIds
                      .stream()
                      .filter(java.util.Objects::nonNull)
                      .collect(
                          java.util.stream.Collectors.toCollection(
                              java.util.LinkedHashSet::new
                          )
                      );
        if (ids.isEmpty()) throw new BadRequestException(
            "Hãy chọn ít nhất một thuộc tính"
        );
        var found = variants.findByVariantIdIn(ids);
        if (
            found.size() != ids.size() ||
            found
                .stream()
                .anyMatch(v -> !v.getProduct().getProductId().equals(productId))
        ) {
            throw new BadRequestException(
                "Thuộc tính được chọn không thuộc sản phẩm"
            );
        }
        var links = found
            .stream()
            .map(variant -> {
                var link = new PromotionVariant();
                link.setId(new PromotionVariantId(id, variant.getVariantId()));
                link.setPromotion(promotion);
                link.setVariant(variant);
                return link;
            })
            .toList();
        variantAssociations.saveAll(links);
        variantAssociations.flush();
    }

    @Transactional
    public void removeProduct(Long id, Long productId) {
        lock(id);
        var association = associations
            .findById(new PromotionProductId(id, productId))
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Không tìm thấy liên kết Promotion/Product"
                )
            );
        clearVariantScope(id, productId);
        associations.delete(association);
        associations.flush();
    }

    private void clearVariantScope(Long promotionId, Long productId) {
        var selected = variantAssociations.findSelectedVariantIds(
            promotionId,
            productId
        );
        if (!selected.isEmpty()) {
            variantAssociations.deleteSelected(promotionId, selected);
            variantAssociations.flush();
        }
    }

    private Promotion lock(Long id) {
        return promotions
            .findByIdForUpdate(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy Promotion")
            );
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
