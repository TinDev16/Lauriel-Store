package com.lauriel.store.service;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.PurchaseReceiptStatus;
import com.lauriel.store.exception.*;
import com.lauriel.store.mapper.InventoryMapper;
import com.lauriel.store.repository.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseReceiptService {

    private static final BigDecimal MAX_TOTAL = new BigDecimal(
        "999999999999.99"
    );
    private final PurchaseReceiptRepository receipts;
    private final PurchaseReceiptDetailRepository items;
    private final SupplierRepository suppliers;
    private final ProductVariantRepository variants;
    private final UserRepository users;
    private final InventoryService inventory;

    public PurchaseReceiptService(
        PurchaseReceiptRepository receipts,
        PurchaseReceiptDetailRepository items,
        SupplierRepository suppliers,
        ProductVariantRepository variants,
        UserRepository users,
        InventoryService inventory
    ) {
        this.receipts = receipts;
        this.items = items;
        this.suppliers = suppliers;
        this.variants = variants;
        this.users = users;
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseReceiptSummaryResponse> list(
        int page,
        int size
    ) {
        return list(page, size, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseReceiptSummaryResponse> list(
        int page,
        int size,
        String keyword,
        PurchaseReceiptStatus status,
        java.time.LocalDateTime from,
        java.time.LocalDateTime to
    ) {
        return InventoryPagination.response(
            filteredPage(page, size, keyword, status, from, to),
            InventoryMapper::summary
        );
    }

    @Transactional(readOnly = true)
    public long countByStatus(PurchaseReceiptStatus status) {
        if (status == null) {
            return receipts.count();
        }
        return receipts.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return Math.max(0, receipts.count() - receipts.countByStatus(PurchaseReceiptStatus.CANCELLED));
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseReceiptListResponse> listDetailed(
        int page,
        int size,
        String keyword,
        PurchaseReceiptStatus status,
        java.time.LocalDateTime from,
        java.time.LocalDateTime to
    ) {
        return listDetailed(page, size, keyword, status, status == null, from, to);
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseReceiptListResponse> listDetailed(
        int page,
        int size,
        String keyword,
        PurchaseReceiptStatus status,
        boolean excludeCancelled,
        java.time.LocalDateTime from,
        java.time.LocalDateTime to
    ) {
        var result = filteredPage(page, size, keyword, status, excludeCancelled, from, to);
        var receiptIds = result
            .getContent()
            .stream()
            .map(PurchaseReceipt::getReceiptId)
            .toList();
        var receiptItems = receiptIds.isEmpty()
            ? java.util.List.<PurchaseReceiptDetail>of()
            : items.findListItems(receiptIds);
        var itemsByReceipt = receiptItems
            .stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    item -> item.getId().getReceiptId(),
                    java.util.LinkedHashMap::new,
                    java.util.stream.Collectors.toList()
                )
            );
        return InventoryPagination.response(result, receipt ->
            InventoryMapper.listItem(
                receipt,
                itemsByReceipt.getOrDefault(
                    receipt.getReceiptId(),
                    java.util.List.of()
                )
            )
        );
    }

    private org.springframework.data.domain.Page<PurchaseReceipt> filteredPage(
        int page,
        int size,
        String keyword,
        PurchaseReceiptStatus status,
        java.time.LocalDateTime from,
        java.time.LocalDateTime to
    ) {
        return filteredPage(page, size, keyword, status, status == null, from, to);
    }

    private org.springframework.data.domain.Page<PurchaseReceipt> filteredPage(
        int page,
        int size,
        String keyword,
        PurchaseReceiptStatus status,
        boolean excludeCancelled,
        java.time.LocalDateTime from,
        java.time.LocalDateTime to
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(
                "Thời gian bắt đầu phải trước hoặc bằng thời gian kết thúc"
            );
        }
        var filter = receiptFilter(keyword, status, excludeCancelled, from, to);
        return receipts.findAll(
            filter,
            InventoryPagination.page(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt", "receiptId")
            )
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<PurchaseReceipt> listForExport(
        String keyword,
        PurchaseReceiptStatus status,
        java.time.LocalDateTime from,
        java.time.LocalDateTime to
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Thời gian bắt đầu phải trước hoặc bằng thời gian kết thúc");
        }
        var filter = receiptFilter(keyword, status, from, to);
        return receipts.findAll(
            filter,
            Sort.by(Sort.Direction.DESC, "createdAt", "receiptId")
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<PurchaseReceiptDetail> listDetailsForExport(java.util.Collection<Long> receiptIds) {
        if (receiptIds == null || receiptIds.isEmpty()) return java.util.List.of();
        var ids = new java.util.ArrayList<>(receiptIds);
        var result = new java.util.ArrayList<PurchaseReceiptDetail>();
        int batchSize = 500;
        for (int i = 0; i < ids.size(); i += batchSize) {
            var batch = ids.subList(i, Math.min(i + batchSize, ids.size()));
            result.addAll(items.findListItems(batch));
        }
        return result;
    }

    public org.springframework.data.jpa.domain.Specification<PurchaseReceipt> receiptFilter(
        String keyword,
        PurchaseReceiptStatus status,
        java.time.LocalDateTime from,
        java.time.LocalDateTime to
    ) {
        return receiptFilter(keyword, status, status == null, from, to);
    }

    public org.springframework.data.jpa.domain.Specification<PurchaseReceipt> receiptFilter(
        String keyword,
        PurchaseReceiptStatus status,
        boolean excludeCancelled,
        java.time.LocalDateTime from,
        java.time.LocalDateTime to
    ) {
        String term =
            keyword == null || keyword.isBlank()
                ? null
                : keyword.strip().toLowerCase(java.util.Locale.ROOT);
        return (root, query, builder) -> {
            var predicates =
                new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            } else if (excludeCancelled) {
                predicates.add(builder.notEqual(root.get("status"), PurchaseReceiptStatus.CANCELLED));
            }
            if (from != null) predicates.add(
                builder.greaterThanOrEqualTo(root.get("createdAt"), from)
            );
            if (to != null) predicates.add(
                builder.lessThanOrEqualTo(root.get("createdAt"), to)
            );
            if (term != null) {
                String pattern = "%" + term + "%";
                var supplierName = builder.like(
                    builder.lower(root.get("supplier").get("supplierName")),
                    pattern
                );
                var username = builder.like(
                    builder.lower(root.get("user").get("username")),
                    pattern
                );
                var fullName = builder.like(
                    builder.lower(root.get("user").get("fullName")),
                    pattern
                );
                jakarta.persistence.criteria.Predicate match = builder.or(
                    supplierName,
                    username,
                    fullName
                );
                try {
                    match = builder.or(
                        match,
                        builder.equal(
                            root.get("receiptId"),
                            Long.valueOf(term.replace("pn-", ""))
                        )
                    );
                } catch (NumberFormatException ignored) {
                    // The keyword is textual.
                }
                predicates.add(match);
            }
            return builder.and(
                predicates.toArray(
                    jakarta.persistence.criteria.Predicate[]::new
                )
            );
        };
    }

    @Transactional(readOnly = true)
    public PurchaseReceiptResponse detail(Long id) {
        var receipt = receipts
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy phiếu nhập")
            );
        return response(receipt);
    }

    @Transactional
    public PurchaseReceiptResponse create(
        PurchaseReceiptCreateRequest request,
        Long actorId
    ) {
        var receipt = new PurchaseReceipt();
        receipt.setSupplier(
            suppliers
                .findById(request.supplierId())
                .orElseThrow(() ->
                    new ResourceNotFoundException("Không tìm thấy nhà cung cấp")
                )
        );
        receipt.setUser(requireActor(actorId));
        receipt.setStatus(PurchaseReceiptStatus.DRAFT); // Exact SQL enum: there is no PENDING.
        receipt.setTotalAmount(BigDecimal.ZERO);
        receipts.saveAndFlush(receipt);
        return response(receipt);
    }

    @Transactional
    public PurchaseReceiptResponse addItem(
        Long id,
        PurchaseReceiptDetailRequest request
    ) {
        var receipt = lockDraft(id);
        var key = new PurchaseReceiptDetailId(id, request.variantId());
        if (items.existsById(key)) {
            throw new DuplicateResourceException(
                "Thuộc tính đã có trong phiếu nhập"
            );
        }
        var variant = variants
            .findById(request.variantId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy thuộc tính")
            );
        var item = new PurchaseReceiptDetail();
        item.setId(key);
        item.setPurchaseReceipt(receipt);
        item.setProductVariant(variant);
        apply(item, request.quantity(), request.unitPrice());
        // Validate the aggregate BEFORE persisting an out-of-range total.
        receipt.setTotalAmount(
            checkedTotal(receipt.getTotalAmount().add(item.getSubtotal()))
        );
        items.saveAndFlush(item);
        receipts.saveAndFlush(receipt);
        return response(receipt);
    }

    @Transactional
    public PurchaseReceiptResponse updateItem(
        Long id,
        Long variantId,
        PurchaseReceiptItemUpdateRequest request
    ) {
        var receipt = lockDraft(id);
        var item = requireItem(id, variantId);
        BigDecimal old = item.getSubtotal();
        apply(item, request.quantity(), request.unitPrice());
        receipt.setTotalAmount(
            checkedTotal(
                receipt.getTotalAmount().subtract(old).add(item.getSubtotal())
            )
        );
        items.saveAndFlush(item);
        receipts.saveAndFlush(receipt);
        return response(receipt);
    }

    @Transactional
    public PurchaseReceiptResponse deleteItem(Long id, Long variantId) {
        var receipt = lockDraft(id);
        var item = requireItem(id, variantId);
        receipt.setTotalAmount(
            checkedTotal(receipt.getTotalAmount().subtract(item.getSubtotal()))
        );
        items.delete(item);
        items.flush();
        receipts.saveAndFlush(receipt);
        return response(receipt);
    }

    @Transactional
    public PurchaseReceiptResponse complete(Long id, Long actorId) {
        var receipt = lockDraft(id); // Serialize all edits/completions of the same receipt.
        var details = items.findItemsForCompletion(id); // Do not fetch variants until they are locked.
        if (details.isEmpty()) {
            throw new ResourceConflictException(
                "Phiếu nhập phải có ít nhất một dòng hàng"
            );
        }
        BigDecimal total = BigDecimal.ZERO;
        for (var item : details) {
            apply(item, item.getQuantity(), item.getUnitPrice()); // Recompute server-owned amounts.
            total = checkedTotal(total.add(item.getSubtotal()));
        }
        inventory.importReceipt(id, details, requireActor(actorId));
        receipt.setTotalAmount(total);
        receipt.setStatus(PurchaseReceiptStatus.COMPLETED);
        receipts.saveAndFlush(receipt);
        return response(receipt);
    }

    @Transactional
    public PurchaseReceiptResponse cancel(Long id) {
        var receipt = lockDraft(id);
        receipt.setStatus(PurchaseReceiptStatus.CANCELLED);
        receipts.saveAndFlush(receipt);
        return response(receipt);
    }

    private PurchaseReceipt lockDraft(Long id) {
        var receipt = receipts
            .findByIdForUpdate(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy phiếu nhập")
            );
        if (receipt.getStatus() != PurchaseReceiptStatus.DRAFT) {
            throw new ResourceConflictException(
                "Chỉ phiếu nhập DRAFT được sửa, hoàn tất hoặc hủy"
            );
        }
        return receipt;
    }

    private PurchaseReceiptDetail requireItem(Long id, Long variantId) {
        return items
            .findById(new PurchaseReceiptDetailId(id, variantId))
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy dòng hàng")
            );
    }

    private User requireActor(Long id) {
        return users
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy người dùng")
            );
    }

    private void apply(
        PurchaseReceiptDetail item,
        int quantity,
        BigDecimal unitPrice
    ) {
        if (
            quantity <= 0 ||
            unitPrice == null ||
            unitPrice.signum() < 0 ||
            unitPrice.scale() > 2 ||
            unitPrice.compareTo(new BigDecimal("9999999999.99")) > 0
        ) {
            throw new BadRequestException("Số lượng hoặc đơn giá không hợp lệ");
        }
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setSubtotal(
            checkedTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)))
        );
    }

    private BigDecimal checkedTotal(BigDecimal total) {
        if (total.signum() < 0 || total.compareTo(MAX_TOTAL) > 0) {
            throw new BadRequestException(
                "Tổng tiền vượt giới hạn DECIMAL(14,2)"
            );
        }
        return total;
    }

    private PurchaseReceiptResponse response(PurchaseReceipt receipt) {
        return InventoryMapper.receipt(
            receipt,
            items.findById_ReceiptIdOrderById_VariantIdAsc(
                receipt.getReceiptId()
            )
        );
    }
}
