package com.lauriel.store.controller.api.staff;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.PurchaseReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/purchase-receipts")
public class PurchaseReceiptController {

    private final PurchaseReceiptService receipts;

    public PurchaseReceiptController(PurchaseReceiptService receipts) {
        this.receipts = receipts;
    }

    @GetMapping
    public PageResponse<PurchaseReceiptSummaryResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return receipts.list(page, size);
    }

    @GetMapping("/{id}")
    public PurchaseReceiptResponse detail(@PathVariable @Positive Long id) {
        return receipts.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseReceiptResponse create(
        @Valid @RequestBody PurchaseReceiptCreateRequest request,
        @AuthenticationPrincipal StoreUserDetails user
    ) {
        return receipts.create(request, user.getUserId());
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseReceiptResponse addItem(
        @PathVariable @Positive Long id,
        @Valid @RequestBody PurchaseReceiptDetailRequest request
    ) {
        return receipts.addItem(id, request);
    }

    @PutMapping("/{id}/items/{variantId}")
    public PurchaseReceiptResponse updateItem(
        @PathVariable @Positive Long id,
        @PathVariable @Positive Long variantId,
        @Valid @RequestBody PurchaseReceiptItemUpdateRequest request
    ) {
        return receipts.updateItem(id, variantId, request);
    }

    @DeleteMapping("/{id}/items/{variantId}")
    public PurchaseReceiptResponse deleteItem(
        @PathVariable @Positive Long id,
        @PathVariable @Positive Long variantId
    ) {
        return receipts.deleteItem(id, variantId);
    }

    @PostMapping("/{id}/complete")
    public PurchaseReceiptResponse complete(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal StoreUserDetails user
    ) {
        return receipts.complete(id, user.getUserId());
    }

    @PostMapping("/{id}/cancel")
    public PurchaseReceiptResponse cancel(@PathVariable @Positive Long id) {
        return receipts.cancel(id);
    }
}
