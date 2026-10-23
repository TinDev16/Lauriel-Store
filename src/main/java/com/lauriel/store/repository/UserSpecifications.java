package com.lauriel.store.repository;

import com.lauriel.store.entity.User;
import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.security.StoreRole;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<User> filter(
        String keyword,
        StoreRole role,
        UserStatus status
    ) {
        return (root, query, cb) -> {
            var predicates =
                new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern =
                    "%" + keyword.strip().toLowerCase(Locale.ROOT) + "%";
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern)
                    )
                );
            }
            if (role != null) {
                predicates.add(
                    cb.equal(root.join("role").get("roleName"), role.name())
                );
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(
                predicates.toArray(
                    jakarta.persistence.criteria.Predicate[]::new
                )
            );
        };
    }
}
