package com.lauriel.store.repository;

import com.lauriel.store.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<Color, Long> {
    boolean existsByColorNameIgnoreCase(String name);
    boolean existsByColorNameIgnoreCaseAndColorIdNot(String name, Long id);
}
