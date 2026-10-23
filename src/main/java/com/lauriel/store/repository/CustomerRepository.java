package com.lauriel.store.repository;

import com.lauriel.store.entity.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository
    extends
        JpaRepository<Customer, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Customer>
{
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(
        attributePaths = "user"
    )
    org.springframework.data.domain.Page<Customer> findAll(
        org.springframework.data.domain.Pageable pageable
    );

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(
        attributePaths = "user"
    )
    org.springframework.data.domain.Page<Customer> findAll(
        org.springframework.data.jpa.domain.Specification<Customer> filter,
        org.springframework.data.domain.Pageable pageable
    );

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(
        attributePaths = "user"
    )
    Optional<Customer> findById(Long id);

    Optional<Customer> findByUser_UserId(Long userId);
}
