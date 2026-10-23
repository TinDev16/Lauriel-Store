package com.lauriel.store.controller.api.admin;

import com.lauriel.store.dto.request.SizeRequest;
import com.lauriel.store.dto.response.SizeResponse;
import com.lauriel.store.service.SizeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sizes")
public class AdminSizeController {

    private final SizeService service;

    public AdminSizeController(SizeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SizeResponse create(@Valid @RequestBody SizeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SizeResponse update(
        @PathVariable @Positive Long id,
        @Valid @RequestBody SizeRequest request
    ) {
        return service.update(id, request);
    }
}
