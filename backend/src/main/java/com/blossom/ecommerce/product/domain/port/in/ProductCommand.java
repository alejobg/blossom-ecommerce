package com.blossom.ecommerce.product.domain.port.in;

import com.blossom.ecommerce.product.domain.ProductCategory;

import java.math.BigDecimal;

/**
 * Data needed to create or update a product, decoupled from the REST request DTO.
 */
public record ProductCommand(String name, String description, BigDecimal price, int stock,
                             ProductCategory category) {
}
