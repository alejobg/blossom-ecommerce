package com.blossom.ecommerce.product.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;

import java.math.BigDecimal;

/**
 * Optional search criteria for the catalog. Any null field means "do not filter by it".
 */
public record ProductFilter(String name, ProductCategory category, BigDecimal minPrice, BigDecimal maxPrice) {

    public ProductFilter {
        name = (name == null || name.isBlank()) ? null : name.trim();
        if (minPrice != null && minPrice.signum() < 0) {
            throw new DomainValidationException("minPrice must be greater than or equal to 0");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new DomainValidationException("maxPrice must be greater than or equal to 0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new DomainValidationException("minPrice must be less than or equal to maxPrice");
        }
    }

    public static ProductFilter empty() {
        return new ProductFilter(null, null, null, null);
    }
}
