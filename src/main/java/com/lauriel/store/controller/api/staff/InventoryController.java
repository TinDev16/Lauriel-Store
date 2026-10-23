package com.lauriel.store.controller.api.staff;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.enums.InventoryTransactionType;
import com.lauriel.store.service.InventoryService;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/inventory")
public class InventoryController {

    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public PageResponse<InventoryResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return inventory.list(page, size, null);
    }

    @GetMapping("/low-stock")
    public PageResponse<InventoryResponse> lowStock(
        @RequestParam(defaultValue = "5") @Min(0) int threshold,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return inventory.list(page, size, threshold);
    }

    @GetMapping("/{variantId}")
    public InventoryResponse detail(@PathVariable @Positive Long variantId) {
        return inventory.detail(variantId);
    }

    @GetMapping("/transactions")
    public PageResponse<InventoryTransactionResponse> history(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
        @RequestParam(required = false) @Positive Long variantId,
        @RequestParam(required = false) InventoryTransactionType type,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        ) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        ) LocalDateTime to
    ) {
        return inventory.history(page, size, variantId, type, from, to);
    }
}
