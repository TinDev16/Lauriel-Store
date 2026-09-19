package com.lauriel.store.controller.api.staff;

import com.lauriel.store.dto.request.SupplierRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.service.SupplierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/suppliers")
public class SupplierController {
    private final SupplierService suppliers;

    public SupplierController(SupplierService suppliers) {
        this.suppliers = suppliers;
    }

    @GetMapping
    public PageResponse<SupplierResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        return suppliers.list(page, size);
    }

    @GetMapping("/{id}")
    public SupplierResponse detail(@PathVariable @Positive Long id) {
        return suppliers.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
        return suppliers.create(request);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable @Positive Long id, @Valid @RequestBody SupplierRequest request) {
        return suppliers.update(id, request);
    }
}
