package com.lauriel.store.service;

import com.lauriel.store.dto.request.ProfileUpdateRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.Customer;
import com.lauriel.store.exception.DuplicateResourceException;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.repository.CustomerRepository;
import com.lauriel.store.repository.UserRepository;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customers;
    private final UserRepository users;

    public CustomerService(CustomerRepository customers, UserRepository users) {
        this.customers = customers;
        this.users = users;
    }

    public PageResponse<CustomerResponse> list(int page, int size) {
        return list(page, size, null, null);
    }

    public PageResponse<CustomerResponse> list(
        int page,
        int size,
        String keyword,
        com.lauriel.store.entity.enums.UserStatus status
    ) {
        org.springframework.data.jpa.domain.Specification<Customer> filter = customerFilter(keyword, status);
        return InventoryPagination.response(
            customers.findAll(
                filter,
                InventoryPagination.page(
                    page,
                    size,
                    Sort.by(Sort.Direction.DESC, "customerId")
                )
            ),
            CustomerService::response
        );
    }

    public org.springframework.data.jpa.domain.Specification<Customer> customerFilter(
        String keyword,
        com.lauriel.store.entity.enums.UserStatus status
    ) {
        String term =
            keyword == null || keyword.isBlank()
                ? null
                : "%" +
                  keyword.strip().toLowerCase(java.util.Locale.ROOT) +
                  "%";
        return (root, query, builder) -> {
            var predicates =
                new ArrayList<jakarta.persistence.criteria.Predicate>();
            var user = root.join(
                "user",
                jakarta.persistence.criteria.JoinType.LEFT
            );
            if (status != null) predicates.add(
                builder.equal(user.get("status"), status)
            );
            if (term != null) predicates.add(
                builder.or(
                    builder.like(builder.lower(root.get("customerName")), term),
                    builder.like(builder.lower(root.get("phone")), term),
                    builder.like(builder.lower(user.get("username")), term),
                    builder.like(builder.lower(user.get("email")), term)
                )
            );
            return builder.and(
                predicates.toArray(
                    jakarta.persistence.criteria.Predicate[]::new
                )
            );
        };
    }

    public List<Customer> listForExport(
        String keyword,
        com.lauriel.store.entity.enums.UserStatus status
    ) {
        return customers.findAll(
            customerFilter(keyword, status),
            Sort.by(Sort.Direction.DESC, "customerId")
        );
    }

    public CustomerResponse detail(Long id) {
        return response(
            customers
                .findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Không tìm thấy khách hàng")
                )
        );
    }

    public ProfileResponse me(Long userId) {
        return profile(requireByUser(userId));
    }

    @Transactional
    public ProfileResponse updateMe(Long userId, ProfileUpdateRequest request) {
        Customer customer = requireByUser(userId);
        var user = customer.getUser();
        String email = normalizeNullable(request.email());
        if (
            email != null &&
            users.existsByEmailIgnoreCaseAndUserIdNot(email, userId)
        ) {
            throw new DuplicateResourceException(
                "Email đã được sử dụng bởi tài khoản khác"
            );
        }
        String name = request.fullName().strip();
        String phone = normalizeNullable(request.phone());
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        customer.setCustomerName(name);
        customer.setPhone(phone);
        customer.setAddress(normalizeNullable(request.address()));
        users.save(user);
        customers.saveAndFlush(customer);
        return profile(customer);
    }

    public Map<Long, String> names(Collection<Long> ids) {
        Map<Long, String> result = new HashMap<>();
        if (!ids.isEmpty()) customers
            .findAllById(ids)
            .forEach(c -> result.put(c.getCustomerId(), c.getCustomerName()));
        return result;
    }

    private Customer requireByUser(Long userId) {
        return customers
            .findByUser_UserId(userId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy hồ sơ khách hàng")
            );
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) return null;
        return value.strip();
    }

    private static ProfileResponse profile(Customer c) {
        var u = c.getUser();
        return new ProfileResponse(
            c.getCustomerId(),
            u.getUserId(),
            u.getUsername(),
            u.getFullName(),
            u.getEmail(),
            c.getPhone(),
            c.getAddress()
        );
    }

    private static CustomerResponse response(Customer c) {
        var u = c.getUser();
        return new CustomerResponse(
            c.getCustomerId(),
            u == null ? null : u.getUsername(),
            c.getCustomerName(),
            u == null ? null : u.getEmail(),
            c.getPhone(),
            u == null ? null : u.getStatus(),
            c.getCreatedAt(),
            c.getAddress()
        );
    }
}
