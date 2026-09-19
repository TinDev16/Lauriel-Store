package com.lauriel.store.repository;

import com.lauriel.store.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size, Long> {
    boolean existsBySizeNameIgnoreCase(String name);
    boolean existsBySizeNameIgnoreCaseAndSizeIdNot(String name, Long id);
}
