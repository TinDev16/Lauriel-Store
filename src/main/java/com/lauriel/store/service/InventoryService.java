package com.lauriel.store.service;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.InventoryTransactionType;
import com.lauriel.store.exception.*;
import com.lauriel.store.mapper.InventoryMapper;
import com.lauriel.store.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class InventoryService {
    @PersistenceContext
    private EntityManager entityManager;
    private final ProductVariantRepository variants;
    private final InventoryTransactionRepository transactions;

    public InventoryService(ProductVariantRepository variants, InventoryTransactionRepository transactions) {
        this.variants = variants;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> list(int page, int size, Integer threshold) {
        if (threshold != null && threshold < 0) {
            throw new BadRequestException("threshold phải không âm");
        }
        Specification<ProductVariant> filter = (root, query, builder) -> threshold == null
                ? builder.conjunction() : builder.lessThanOrEqualTo(root.get("quantity"), threshold);
        return InventoryPagination.response(variants.findAll(filter, InventoryPagination.page(page, size,
                Sort.by("variantId"))), InventoryMapper::inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> selectableVariants() {
        return variants.findAllForSelection().stream().map(InventoryMapper::inventory).toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse detail(Long id) {
        return InventoryMapper.inventory(variants.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể")));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> history(int page, int size, Long variantId,
            InventoryTransactionType type, LocalDateTime from, LocalDateTime to) {
        if (variantId != null && variantId <= 0) {
            throw new BadRequestException("variantId phải là số dương");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from phải trước hoặc bằng to");
        }
        Specification<InventoryTransaction> filter = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (variantId != null) predicates.add(builder.equal(root.get("productVariant").get("variantId"), variantId));
            if (type != null) predicates.add(builder.equal(root.get("transactionType"), type));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), to));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return InventoryPagination.response(transactions.findAll(filter, InventoryPagination.page(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "transactionId"))), InventoryMapper::history);
    }

    // Internal operation: callers must own the receipt write lock and an existing transaction.
    // There is deliberately no endpoint accepting an arbitrary replacement stock quantity.
    @Transactional(propagation = Propagation.MANDATORY)
    public void importReceipt(Long receiptId, List<PurchaseReceiptDetail> details, User performedBy) {
        var ordered = details.stream().sorted(Comparator.comparing(d -> d.getId().getVariantId())).toList();
        Map<Long, ProductVariant> locked = new LinkedHashMap<>();
        for (var detail : ordered) {
            Long id = detail.getId().getVariantId();
            var variant = variants.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể"));
            long updated = (long) variant.getQuantity() + detail.getQuantity();
            if (detail.getQuantity() <= 0 || updated > Integer.MAX_VALUE || variant.getQuantity() < 0) {
                throw new ResourceConflictException("Số lượng tồn kho vượt giới hạn hợp lệ");
            }
            locked.put(id, variant);
        }
        for (var detail : ordered) {
            var variant = locked.get(detail.getId().getVariantId());
            variant.setQuantity(Math.addExact(variant.getQuantity(), detail.getQuantity()));
            var transaction = new InventoryTransaction();
            transaction.setProductVariant(variant);
            transaction.setUser(performedBy);
            transaction.setTransactionType(InventoryTransactionType.IMPORT);
            transaction.setQuantity(detail.getQuantity());
            transaction.setReferenceType("PURCHASE_RECEIPT");
            transaction.setReferenceId(receiptId);
            // Flush within the owning receipt transaction; failures roll back stock and all history.
            transactions.saveAndFlush(transaction);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, ProductVariant> lockForSale(Map<Long, Integer> quantities) {
        Map<Long, ProductVariant> locked = new LinkedHashMap<>();
        for (var entry : new TreeMap<>(quantities).entrySet()) {
            var variant = variants.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể"));
            if (variant.getStatus() != ActiveStatus.ACTIVE || variant.getProduct().getStatus() != ActiveStatus.ACTIVE) {
                throw new ResourceConflictException("Sản phẩm không còn hoạt động");
            }
            requireSaleQuantity(variant, entry.getValue());
            locked.put(entry.getKey(), variant);
        }
        return locked;
    }

    // Reuse the rows locked during pricing. No repeated variant lock/query is needed here.
    @Transactional(propagation = Propagation.MANDATORY)
    public void decreaseForSale(Long orderId, List<OrderDetail> details, User performedBy) {
        var ordered = details.stream().sorted(Comparator.comparing(d -> d.getId().getVariantId())).toList();
        for (var detail : ordered) {
            var variant = detail.getProductVariant();
            if (!entityManager.contains(variant)
                    || entityManager.getLockMode(variant) != LockModeType.PESSIMISTIC_WRITE) {
                throw new IllegalStateException("Không thể thay đổi tồn kho khi biến thể chưa được khóa để cập nhật");
            }
            requireSaleQuantity(variant, detail.getQuantity());
        }
        for (var detail : ordered) {
            var variant = detail.getProductVariant();
            variant.setQuantity(variant.getQuantity() - detail.getQuantity());
            recordOrderMovement(orderId, variant, detail.getQuantity(), InventoryTransactionType.SALE, performedBy);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreForCancellation(Long orderId, List<OrderDetail> details, User performedBy) {
        requireRecordedSale(orderId, details);
        var ordered = details.stream().sorted(Comparator.comparing(d -> d.getId().getVariantId())).toList();
        Map<Long, ProductVariant> locked = new LinkedHashMap<>();
        for (var detail : ordered) {
            Long id = detail.getId().getVariantId();
            var variant = variants.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể"));
            long restored = (long) variant.getQuantity() + detail.getQuantity();
            if (detail.getQuantity() <= 0 || variant.getQuantity() < 0 || restored > Integer.MAX_VALUE) {
                throw new ResourceConflictException("Số lượng tồn kho vượt giới hạn hợp lệ");
            }
            locked.put(id, variant);
        }
        for (var detail : ordered) {
            var variant = locked.get(detail.getId().getVariantId());
            variant.setQuantity(Math.addExact(variant.getQuantity(), detail.getQuantity()));
            recordOrderMovement(orderId, variant, detail.getQuantity(), InventoryTransactionType.RETURN, performedBy);
        }
    }

    private void requireRecordedSale(Long orderId, List<OrderDetail> details) {
        Map<Long, Integer> expected = new TreeMap<>();
        for (var detail : details) expected.put(detail.getId().getVariantId(), detail.getQuantity());
        Map<Long, Integer> sold = new TreeMap<>();
        try {
            for (var sale : transactions.findByReferenceTypeAndReferenceIdAndTransactionType(
                    "ORDER", orderId, InventoryTransactionType.SALE)) {
                sold.merge(sale.getProductVariant().getVariantId(), sale.getQuantity(), Math::addExact);
            }
        } catch (ArithmeticException overflow) {
            throw new ResourceConflictException("Lịch sử SALE của đơn hàng không hợp lệ");
        }
        if (!expected.equals(sold)) {
            throw new ResourceConflictException("Không thể hoàn kho khi đơn hàng chưa có lịch sử SALE đầy đủ");
        }
    }
    private void requireSaleQuantity(ProductVariant variant, int requested) {
        if (requested <= 0) {
            throw new BadRequestException("Số lượng mua phải dương");
        }
        if (variant.getQuantity() < requested) {
            throw new ResourceConflictException("Sản phẩm không đủ tồn kho.");
        }
    }

    private void recordOrderMovement(Long orderId, ProductVariant variant, int quantity,
            InventoryTransactionType type, User performedBy) {
        var transaction = new InventoryTransaction();
        transaction.setProductVariant(variant);
        transaction.setUser(performedBy);
        transaction.setTransactionType(type);
        transaction.setQuantity(quantity); // Always positive; type determines the direction.
        transaction.setReferenceType("ORDER");
        transaction.setReferenceId(orderId);
        transactions.saveAndFlush(transaction);
    }
}
