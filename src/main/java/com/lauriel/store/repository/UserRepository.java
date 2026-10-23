package com.lauriel.store.repository;

import com.lauriel.store.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository
    extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>
{
    @EntityGraph(attributePaths = "role")
    Optional<User> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Long userId);

    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Long userId);

    long countByRole_RoleNameAndStatus(
        String roleName,
        com.lauriel.store.entity.enums.UserStatus status
    );
    boolean existsByRole_RoleName(String roleName);

    @Override
    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Specification<User> specification, Pageable pageable);
}
