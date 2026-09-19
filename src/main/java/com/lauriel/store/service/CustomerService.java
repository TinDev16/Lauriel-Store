package com.lauriel.store.service;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.Customer;
import com.lauriel.store.repository.CustomerRepository;
import com.lauriel.store.exception.ResourceNotFoundException;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly = true)
public class CustomerService {
    private final CustomerRepository customers;
    public CustomerService(CustomerRepository customers) { this.customers = customers; }
    public PageResponse<CustomerResponse> list(int page, int size) {
        return InventoryPagination.response(customers.findAll(InventoryPagination.page(page, size,
                Sort.by(Sort.Direction.DESC, "customerId"))), CustomerService::response);
    }
    public CustomerResponse detail(Long id) {
        return response(customers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng")));
    }
    public Map<Long, String> names(Collection<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        if (!ids.isEmpty()) customers.findAllById(ids).forEach(c -> result.put(c.getCustomerId(), c.getCustomerName()));
        return result;
    }
    private static CustomerResponse response(Customer c) {
        var u = c.getUser();
        return new CustomerResponse(c.getCustomerId(), u == null ? null : u.getUsername(), c.getCustomerName(),
                u == null ? null : u.getEmail(), c.getPhone(), u == null ? null : u.getStatus(),
                c.getCreatedAt(), c.getAddress());
    }
}
