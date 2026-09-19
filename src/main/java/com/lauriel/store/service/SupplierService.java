package com.lauriel.store.service;

import com.lauriel.store.dto.request.SupplierRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.Supplier;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.mapper.InventoryMapper;
import com.lauriel.store.repository.SupplierRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {
    private final SupplierRepository suppliers;

    public SupplierService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> list(int page, int size) {
        return InventoryPagination.response(suppliers.findAll(InventoryPagination.page(page, size,
                Sort.by("supplierId"))), InventoryMapper::supplier);
    }

    @Transactional(readOnly = true)
    public SupplierResponse detail(Long id) {
        return InventoryMapper.supplier(require(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = new Supplier();
        apply(supplier, request);
        return InventoryMapper.supplier(suppliers.saveAndFlush(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        var supplier = require(id);
        apply(supplier, request);
        return InventoryMapper.supplier(suppliers.saveAndFlush(supplier));
    }

    @Transactional
    public SupplierResponse updateStatus(Long id,com.lauriel.store.entity.enums.ActiveStatus status) {
        var supplier=require(id); supplier.setStatus(status);
        return InventoryMapper.supplier(suppliers.saveAndFlush(supplier));
    }
    private Supplier require(Long id) {
        return suppliers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));
    }

    private void apply(Supplier supplier, SupplierRequest request) {
        supplier.setSupplierName(request.supplierName().strip());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setStatus(request.status());
    }
}
