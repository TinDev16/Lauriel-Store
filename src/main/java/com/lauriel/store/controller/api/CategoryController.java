package com.lauriel.store.controller.api;

import com.lauriel.store.dto.response.CategorySummaryResponse;
import com.lauriel.store.service.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategorySummaryResponse> list() {
        return service.listActive();
    }
}
