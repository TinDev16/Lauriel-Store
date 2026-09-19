package com.lauriel.store.service;

import com.lauriel.store.dto.request.ProductRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.Product;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.mapper.CatalogMapper;
import com.lauriel.store.repository.ProductRepository;
import com.lauriel.store.repository.ProductSpecifications;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private static final Set<String> SORT_FIELDS = Set.of("price", "productName", "createdAt");
    private final ProductRepository productRepository;
    private final PricingService pricing;

    public ProductService(ProductRepository productRepository, PricingService pricing) {
        this.productRepository = productRepository;
        this.pricing = pricing;
    }

    // Preserve the foundation read method; public endpoints use getPublicDetail.
    @Transactional(readOnly = true)
    public ProductResponse getById(Long productId) {
        return CatalogMapper.product(requireProduct(productId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listPublic(int page, int size, String keyword,
            Long sizeId, Long colorId, String sortBy, String direction) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new BadRequestException("Phân trang không hợp lệ; size phải từ 1 đến 100");
        }
        if ((sizeId != null && sizeId <= 0) || (colorId != null && colorId <= 0)) {
            throw new BadRequestException("sizeId/colorId phải là số dương");
        }
        if (!SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("sortBy chỉ hỗ trợ price, productName, createdAt");
        }
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("direction chỉ hỗ trợ asc hoặc desc");
        }
        var pageable = PageRequest.of(page, size,
                Sort.by(sortDirection, sortBy).and(Sort.by("productId")));
        // DTO projection selects summary columns only, without description or variant collections.
        var products = productRepository.findBy(ProductSpecifications.activeCatalog(keyword, sizeId, colorId),
                query -> query.as(ProductSummaryResponse.class).page(pageable));
        return new PageResponse<>(products.getContent(), products.getNumber(), products.getSize(),
                products.getTotalElements(), products.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicDetail(Long productId) {
        var product = productRepository.findByProductIdAndStatus(productId, ActiveStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        var variants = pricing.publicVariants(productId);
        return new ProductDetailResponse(product.getProductId(), product.getProductName(),
                product.getDescription(), product.getPrice(), product.getImgUrl(), product.getStatus(), variants);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return CatalogMapper.product(productRepository.saveAndFlush(product));
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest request) {
        var product = requireProduct(productId);
        apply(product, request);
        return CatalogMapper.product(productRepository.saveAndFlush(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long productId, ActiveStatus status) {
        var product = requireProduct(productId);
        product.setStatus(status);
        return CatalogMapper.product(productRepository.saveAndFlush(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listAdmin(int page, int size, String keyword, ActiveStatus status) {
        org.springframework.data.jpa.domain.Specification<Product> filter = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (keyword != null && !keyword.isBlank()) {
                String escaped = keyword.strip().toLowerCase(java.util.Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + escaped + "%", '\\'));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = productRepository.findBy(filter, query -> query.as(ProductSummaryResponse.class)
                .page(InventoryPagination.page(page,size,Sort.by(Sort.Direction.DESC,"productId"))));
        return new PageResponse<>(result.getContent(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
    }

    private void apply(Product product, ProductRequest request) {
        product.setProductName(request.productName().strip());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImgUrl(request.imgUrl());
        product.setStatus(request.status());
    }
}
