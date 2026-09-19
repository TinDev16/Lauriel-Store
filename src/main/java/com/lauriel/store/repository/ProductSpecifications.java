package com.lauriel.store.repository;

import com.lauriel.store.entity.Product;
import com.lauriel.store.entity.ProductVariant;
import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {
    private ProductSpecifications() {
    }

    public static Specification<Product> activeCatalog(String keyword, Long sizeId, Long colorId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("status"), ActiveStatus.ACTIVE));
            if (keyword != null && !keyword.isBlank()) {
                String literal = keyword.strip().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                String pattern = "%" + literal + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("productName")), pattern, '!'),
                        builder.like(builder.lower(root.get("description")), pattern, '!')));
            }
            if (sizeId != null || colorId != null) {
                // A single EXISTS checks both attributes on the same active variant.
                // It avoids duplicate product rows and collection fetches during pagination.
                var variants = query.subquery(Long.class);
                var variant = variants.from(ProductVariant.class);
                List<Predicate> matches = new ArrayList<>();
                matches.add(builder.equal(variant.get("product"), root));
                matches.add(builder.equal(variant.get("status"), ActiveStatus.ACTIVE));
                if (sizeId != null) {
                    matches.add(builder.equal(variant.get("size").get("sizeId"), sizeId));
                }
                if (colorId != null) {
                    matches.add(builder.equal(variant.get("color").get("colorId"), colorId));
                }
                variants.select(variant.get("variantId"))
                        .where(matches.toArray(Predicate[]::new));
                predicates.add(builder.exists(variants));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
