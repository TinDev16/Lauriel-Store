package com.lauriel.store.controller.api.admin;

import com.lauriel.store.dto.request.CategoryRequest;
import com.lauriel.store.dto.request.ProductStatusRequest;
import com.lauriel.store.dto.response.CategoryResponse;
import com.lauriel.store.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    private final CategoryService service;

    public AdminCategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> list() { return service.listAdmin(); }

    @GetMapping("/{id}")
    public CategoryResponse detail(@PathVariable @Positive Long id) { return service.detail(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable @Positive Long id,
            @Valid @RequestBody CategoryRequest request) { return service.update(id, request); }

    @PatchMapping("/{id}/status")
    public CategoryResponse status(@PathVariable @Positive Long id,
            @Valid @RequestBody ProductStatusRequest request) { return service.updateStatus(id, request.status()); }
}
