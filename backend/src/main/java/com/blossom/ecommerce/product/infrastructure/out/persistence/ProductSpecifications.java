package com.blossom.ecommerce.product.infrastructure.out.persistence;

import com.blossom.ecommerce.product.domain.ProductCategory;
import com.blossom.ecommerce.product.domain.ProductFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Translates a domain {@link ProductFilter} into a JPA Criteria query.
 * Each criterion is applied only when present, so filters can be freely combined.
 */
final class ProductSpecifications {

    private static final char ESCAPE = '\\';

    private ProductSpecifications() {
    }

    static Specification<ProductEntity> activeMatching(ProductFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.<Boolean>get("active")));

            if (filter.name() != null) {
                String pattern = "%" + escapeLike(filter.name().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.<String>get("name")), pattern, ESCAPE));
            }
            if (filter.category() != null) {
                predicates.add(cb.equal(root.<ProductCategory>get("category"), filter.category()));
            }
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), filter.maxPrice()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Makes user input literal inside a LIKE pattern (so "50%" does not act as a wildcard). */
    static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
