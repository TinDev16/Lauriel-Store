package com.lauriel.store.service;

import com.lauriel.store.dto.request.OrderPromotionRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.OrderPromotion;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.repository.OrderPromotionRepository;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPromotionService {

    private final OrderPromotionRepository repository;
    private final Clock clock;

    public OrderPromotionService(
        OrderPromotionRepository repository,
        @Qualifier("pricingClock") Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderPromotionResponse> list(
        int page,
        int size,
        String keyword,
        ActiveStatus status
    ) {
        String term =
            keyword == null || keyword.isBlank()
                ? null
                : "%" + keyword.strip().toLowerCase(Locale.ROOT) + "%";
        var filter =
            (org.springframework.data.jpa.domain.Specification<OrderPromotion>) (
                root,
                q,
                b
            ) -> {
                var p = new ArrayList<jakarta.persistence.criteria.Predicate>();
                if (status != null) p.add(b.equal(root.get("status"), status));
                if (term != null) p.add(
                    b.or(
                        b.like(b.lower(root.get("promotionName")), term),
                        b.like(b.lower(root.get("description")), term)
                    )
                );
                return b.and(
                    p.toArray(jakarta.persistence.criteria.Predicate[]::new)
                );
            };
        return InventoryPagination.response(
            repository.findAll(
                filter,
                InventoryPagination.page(
                    page,
                    size,
                    Sort.by(
                        Sort.Order.asc("status"),
                        Sort.Order.desc("minOrderAmount"),
                        Sort.Order.asc("orderPromotionId")
                    )
                )
            ),
            this::response
        );
    }

    @Transactional(readOnly = true)
    public OrderPromotionResponse detail(Long id) {
        return response(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderPromotionResponse> active(int page, int size) {
        return InventoryPagination.response(
            repository.findActive(
                ActiveStatus.ACTIVE,
                java.time.LocalDateTime.now(clock),
                InventoryPagination.page(
                    page,
                    size,
                    Sort.by(
                        Sort.Order.asc("minOrderAmount"),
                        Sort.Order.asc("orderPromotionId")
                    )
                )
            ),
            this::response
        );
    }

    @Transactional
    public OrderPromotionResponse create(OrderPromotionRequest request) {
        var p = new OrderPromotion();
        apply(p, request);
        return response(repository.saveAndFlush(p));
    }

    @Transactional
    public OrderPromotionResponse update(
        Long id,
        OrderPromotionRequest request
    ) {
        var p = repository.findByIdForUpdate(id).orElseThrow(() -> missing());
        apply(p, request);
        return response(repository.saveAndFlush(p));
    }

    @Transactional
    public OrderPromotionResponse updateStatus(Long id, ActiveStatus status) {
        var p = repository.findByIdForUpdate(id).orElseThrow(() -> missing());
        p.setStatus(status);
        return response(repository.saveAndFlush(p));
    }

    private OrderPromotion find(Long id) {
        return repository.findById(id).orElseThrow(() -> missing());
    }

    private ResourceNotFoundException missing() {
        return new ResourceNotFoundException(
            "Không tìm thấy khuyến mãi đơn hàng"
        );
    }

    private void apply(OrderPromotion p, OrderPromotionRequest r) {
        p.setPromotionName(r.promotionName().strip());
        p.setDescription(
            r.description() == null ? null : r.description().strip()
        );
        p.setMinOrderAmount(r.minOrderAmount());
        p.setDiscountPercent(r.discountPercent());
        p.setMaxDiscountAmount(r.maxDiscountAmount());
        p.setStartDate(r.startDate());
        p.setEndDate(r.endDate());
        p.setStatus(r.status());
    }

    private OrderPromotionResponse response(OrderPromotion p) {
        return new OrderPromotionResponse(
            p.getOrderPromotionId(),
            p.getPromotionName(),
            p.getDescription(),
            p.getMinOrderAmount(),
            p.getDiscountPercent(),
            p.getMaxDiscountAmount(),
            p.getStartDate(),
            p.getEndDate(),
            p.getStatus()
        );
    }
}
