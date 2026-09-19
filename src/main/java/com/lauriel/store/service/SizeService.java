package com.lauriel.store.service;

import com.lauriel.store.dto.request.SizeRequest;
import com.lauriel.store.dto.response.SizeResponse;
import com.lauriel.store.entity.Size;
import com.lauriel.store.exception.DuplicateResourceException;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.mapper.CatalogMapper;
import com.lauriel.store.repository.SizeRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SizeService {
    private final SizeRepository repository;

    public SizeService(SizeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SizeResponse> list() {
        return repository.findAll(Sort.by("sizeName").and(Sort.by("sizeId")))
                .stream().map(CatalogMapper::size).toList();
    }

    @Transactional(readOnly = true)
    public SizeResponse detail(Long id) {
        return CatalogMapper.size(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kích cỡ")));
    }

    @Transactional
    public SizeResponse create(SizeRequest request) {
        String name = request.sizeName().strip();
        if (repository.existsBySizeNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Size đã tồn tại");
        }
        Size size = new Size();
        size.setSizeName(name);
        return CatalogMapper.size(repository.saveAndFlush(size));
    }

    @Transactional
    public SizeResponse update(Long sizeId, SizeRequest request) {
        var size = repository.findById(sizeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy size"));
        String name = request.sizeName().strip();
        if (repository.existsBySizeNameIgnoreCaseAndSizeIdNot(name, sizeId)) {
            throw new DuplicateResourceException("Size đã tồn tại");
        }
        size.setSizeName(name);
        return CatalogMapper.size(repository.saveAndFlush(size));
    }
}
