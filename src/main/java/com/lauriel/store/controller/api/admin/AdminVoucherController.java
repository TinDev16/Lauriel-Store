package com.lauriel.store.controller.api.admin;

import com.lauriel.store.dto.request.VoucherRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.service.VoucherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/vouchers")
public class AdminVoucherController {

    private final VoucherService vouchers;

    public AdminVoucherController(VoucherService vouchers) {
        this.vouchers = vouchers;
    }

    @GetMapping
    public PageResponse<VoucherResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return vouchers.list(page, size);
    }

    @GetMapping("/{id}")
    public VoucherResponse detail(@PathVariable @Positive Long id) {
        return vouchers.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VoucherResponse create(@Valid @RequestBody VoucherRequest request) {
        return vouchers.create(request);
    }

    @PutMapping("/{id}")
    public VoucherResponse update(
        @PathVariable @Positive Long id,
        @Valid @RequestBody VoucherRequest request
    ) {
        return vouchers.update(id, request);
    }
}
