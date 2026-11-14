package com.blossom.ecommerce.product.infrastructure.in.rest.dto;

import com.blossom.ecommerce.product.domain.ProductCategory;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, BigDecimal price, int stock,
                              ProductCategory category) {
}
