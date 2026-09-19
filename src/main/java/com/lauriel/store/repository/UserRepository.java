package com.lauriel.store.repository;

import com.lauriel.store.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "role")
    Optional<User> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Long userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
