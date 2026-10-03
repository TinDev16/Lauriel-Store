package com.lauriel.store.service;

import com.lauriel.store.dto.request.ProductRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.Category;
import com.lauriel.store.entity.Product;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.mapper.CatalogMapper;
import com.lauriel.store.repository.ProductRepository;
import com.lauriel.store.repository.ProductImageRepository;
import com.lauriel.store.repository.ProductSpecifications;
import com.lauriel.store.repository.ProductSummaryView;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private static final Set<String> SORT_FIELDS = Set.of("price", "productName", "createdAt");
    private final ProductRepository productRepository;
    private final CategoryService categories;
    private final PricingService pricing;
    private final ProductImageRepository productImages;

    public ProductService(ProductRepository productRepository, CategoryService categories, PricingService pricing,
            ProductImageRepository productImages) {
        this.productRepository = productRepository;
        this.categories = categories;
        this.pricing = pricing;
        this.productImages = productImages;
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long productId) {
        return CatalogMapper.product(requireProduct(productId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listPublic(int page, int size, String keyword,
            Long categoryId, Long sizeId, Long colorId, String sortBy, String direction) {
        validateList(page, size, categoryId, sizeId, colorId, sortBy);
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("direction chỉ hỗ trợ asc hoặc desc");
        }
        var pageable = PageRequest.of(page, size,
                Sort.by(sortDirection, sortBy).and(Sort.by("productId")));
        var result = productRepository.findBy(
                ProductSpecifications.activeCatalog(keyword, categoryId, sizeId, colorId),
                query -> query.as(ProductSummaryView.class).page(pageable));
        var basePrices = result.getContent().stream().collect(java.util.stream.Collectors.toMap(
                ProductSummaryView::getProductId, ProductSummaryView::getPrice));
        var prices = pricing.priceProducts(basePrices, pricing.now());
        var content = result.getContent().stream().map(product -> summary(product,
                prices.get(product.getProductId()))).toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicDetail(Long productId) {
        var product = productRepository.findByProductIdAndStatusAndCategory_Status(
                        productId, ActiveStatus.ACTIVE, ActiveStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        var variants = pricing.publicVariants(productId);
        var images = productImages.findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(productId)
                .stream().map(i -> new ProductImageResponse(i.getProductImageId(), i.getImageUrl(),
                        i.isPrimary(), i.getDisplayOrder(),
                        i.getColor() == null ? null : i.getColor().getColorId(),
                        i.getColor() == null ? null : i.getColor().getColorName())).toList();
        return new ProductDetailResponse(product.getProductId(), product.getProductName(),
                CatalogMapper.category(product.getCategory()), product.getDescription(), product.getPrice(),
                product.getImgUrl(), product.getStatus(), variants, images);
    }


    @Transactional(readOnly = true)
    public java.util.List<ProductSummaryResponse> related(Long productId, int limit) {
        if (limit < 1 || limit > 10) throw new BadRequestException("limit phải từ 1 đến 10");
        var current = productRepository.findByProductIdAndStatusAndCategory_Status(
                        productId, ActiveStatus.ACTIVE, ActiveStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        var related = productRepository.findRelated(productId, current.getCategory().getCategoryId(),
                ActiveStatus.ACTIVE, PageRequest.of(0, limit));
        var basePrices = related.stream().collect(java.util.stream.Collectors.toMap(
                Product::getProductId, Product::getPrice));
        var prices = pricing.priceProducts(basePrices, pricing.now());
        return related.stream().map(product -> {
            var price = prices.get(product.getProductId());
            return new ProductSummaryResponse(product.getProductId(), product.getProductName(),
                    CatalogMapper.category(product.getCategory()), product.getPrice(), product.getImgUrl(),
                    product.getStatus(), price.effectivePrice(), price.promotion());
        }).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request, categories.requireAssignable(request.categoryId()));
        return CatalogMapper.product(productRepository.saveAndFlush(product));
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest request) {
        Product product = requireProduct(productId);
        Category category;
        if (product.getCategory().getCategoryId().equals(request.categoryId())) {
            category = product.getCategory();
        } else {
            category = categories.requireAssignable(request.categoryId());
        }
        apply(product, request, category);
        return CatalogMapper.product(productRepository.saveAndFlush(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long productId, ActiveStatus status) {
        var product = requireProduct(productId);
        product.setStatus(status);
        return CatalogMapper.product(productRepository.saveAndFlush(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listAdmin(int page, int size, String keyword,
            Long categoryId, ActiveStatus status) {
        org.springframework.data.jpa.domain.Specification<Product> filter = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            if (keyword != null && !keyword.isBlank()) {
                String escaped = keyword.strip().toLowerCase(java.util.Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + escaped + "%", '\\'));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var pageable = InventoryPagination.page(page, size, Sort.by(
                Sort.Order.asc("status"), Sort.Order.desc("productId")));
        var result = productRepository.findBy(filter,
                query -> query.as(ProductSummaryView.class).page(pageable));
        var content = result.getContent().stream().map(this::summary).toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private ProductSummaryResponse summary(ProductSummaryView product, PriceCalculation price) {
        var category = product.getCategory();
        return new ProductSummaryResponse(product.getProductId(), product.getProductName(),
                new CategorySummaryResponse(category.getCategoryId(), category.getCategoryName()),
                product.getPrice(), product.getImgUrl(), product.getStatus(),
                price.effectivePrice(), price.promotion());
    }
    private ProductSummaryResponse summary(ProductSummaryView product) {
        var category = product.getCategory();
        return new ProductSummaryResponse(product.getProductId(), product.getProductName(),
                new CategorySummaryResponse(category.getCategoryId(), category.getCategoryName()),
                product.getPrice(), product.getImgUrl(), product.getStatus());
    }
    private void validateList(int page, int size, Long categoryId, Long sizeId, Long colorId, String sortBy) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new BadRequestException("Phân trang không hợp lệ; size phải từ 1 đến 100");
        }
        if ((categoryId != null && categoryId <= 0) || (sizeId != null && sizeId <= 0)
                || (colorId != null && colorId <= 0)) {
            throw new BadRequestException("categoryId/sizeId/colorId phải là số dương");
        }
        if (!SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("sortBy chỉ hỗ trợ price, productName, createdAt");
        }
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
    }

    private void apply(Product product, ProductRequest request, Category category) {
        product.setCategory(category);
        product.setProductName(request.productName().strip());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImgUrl(request.imgUrl());
        product.setStatus(request.status());
    }
}
