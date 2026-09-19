package com.lauriel.store.service;

import com.lauriel.store.dto.request.ColorRequest;
import com.lauriel.store.dto.response.ColorResponse;
import com.lauriel.store.entity.Color;
import com.lauriel.store.exception.DuplicateResourceException;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.mapper.CatalogMapper;
import com.lauriel.store.repository.ColorRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColorService {
    private final ColorRepository repository;

    public ColorService(ColorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ColorResponse> list() {
        return repository.findAll(Sort.by("colorName").and(Sort.by("colorId")))
                .stream().map(CatalogMapper::color).toList();
    }

    @Transactional(readOnly = true)
    public ColorResponse detail(Long id) {
        return CatalogMapper.color(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy màu sắc")));
    }

    @Transactional
    public ColorResponse create(ColorRequest request) {
        String name = request.colorName().strip();
        if (repository.existsByColorNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Color đã tồn tại");
        }
        Color color = new Color();
        color.setColorName(name);
        return CatalogMapper.color(repository.saveAndFlush(color));
    }

    @Transactional
    public ColorResponse update(Long colorId, ColorRequest request) {
        var color = repository.findById(colorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy color"));
        String name = request.colorName().strip();
        if (repository.existsByColorNameIgnoreCaseAndColorIdNot(name, colorId)) {
            throw new DuplicateResourceException("Color đã tồn tại");
        }
        color.setColorName(name);
        return CatalogMapper.color(repository.saveAndFlush(color));
    }
}
