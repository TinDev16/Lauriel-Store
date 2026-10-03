package com.lauriel.store.repository;

import com.lauriel.store.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Supplier> {
}
