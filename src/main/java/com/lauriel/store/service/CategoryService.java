package com.lauriel.store.service;

import com.lauriel.store.dto.request.CategoryRequest;
import com.lauriel.store.dto.response.CategoryResponse;
import com.lauriel.store.dto.response.CategorySummaryResponse;
import com.lauriel.store.entity.Category;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.exception.DuplicateResourceException;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.repository.CategoryRepository;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAdmin() {
        return repository.findAllWithProductCount();
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> listActive() {
        return repository.findByStatusOrderByCategoryNameAscCategoryIdAsc(ActiveStatus.ACTIVE)
                .stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> listSelectable(Long currentCategoryId) {
        var choices = new LinkedHashMap<Long, CategorySummaryResponse>();
        listActive().forEach(item -> choices.put(item.categoryId(), item));
        if (currentCategoryId != null) {
            Category current = require(currentCategoryId);
            choices.putIfAbsent(current.getCategoryId(), summary(current));
        }
        return List.copyOf(choices.values());
    }

    @Transactional(readOnly = true)
    public CategoryResponse detail(Long id) {
        Category category = require(id);
        return repository.findAllWithProductCount().stream()
                .filter(item -> item.categoryId().equals(category.getCategoryId()))
                .findFirst().orElseThrow();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = normalize(request.categoryName());
        if (repository.existsByCategoryNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Danh mục đã tồn tại");
        }
        Category category = new Category();
        category.setCategoryName(name);
        category.setStatus(request.status());
        Category saved = repository.saveAndFlush(category);
        return response(saved, 0);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = require(id);
        String name = normalize(request.categoryName());
        if (repository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(name, id)) {
            throw new DuplicateResourceException("Danh mục đã tồn tại");
        }
        category.setCategoryName(name);
        category.setStatus(request.status());
        Category saved = repository.saveAndFlush(category);
        return response(saved, saved.getProducts().size());
    }

    @Transactional
    public CategoryResponse updateStatus(Long id, ActiveStatus status) {
        Category category = require(id);
        category.setStatus(status);
        Category saved = repository.saveAndFlush(category);
        return response(saved, saved.getProducts().size());
    }

    @Transactional(readOnly = true)
    public Category requireAssignable(Long id) {
        Category category = require(id);
        if (category.getStatus() != ActiveStatus.ACTIVE) {
            throw new com.lauriel.store.exception.BadRequestException("Không thể gán sản phẩm vào danh mục ngừng hoạt động");
        }
        return category;
    }

    private Category require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
    }

    private String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }

    private CategorySummaryResponse summary(Category category) {
        return new CategorySummaryResponse(category.getCategoryId(), category.getCategoryName());
    }

    private CategoryResponse response(Category category, long count) {
        return new CategoryResponse(category.getCategoryId(), category.getCategoryName(),
                category.getStatus(), category.getCreatedAt(), count);
    }
}
