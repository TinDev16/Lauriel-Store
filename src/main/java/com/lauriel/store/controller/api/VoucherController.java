package com.lauriel.store.controller.api;

import com.lauriel.store.dto.request.VoucherValidationRequest;
import com.lauriel.store.dto.response.VoucherValidationResponse;
import com.lauriel.store.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    private final VoucherService vouchers;

    public VoucherController(VoucherService vouchers) {
        this.vouchers = vouchers;
    }

    @GetMapping("/active")
    public com.lauriel.store.dto.response.PageResponse<com.lauriel.store.dto.response.VoucherResponse> active(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size
    ) {
        return vouchers.listAvailable(page, size);
    }

    @PostMapping("/validate")
    public VoucherValidationResponse validate(
        @Valid @RequestBody VoucherValidationRequest request
    ) {
        return vouchers.preview(request);
    }
}
