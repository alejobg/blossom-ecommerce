package com.blossom.ecommerce.order.domain.port.in;

import com.blossom.ecommerce.shared.domain.DomainValidationException;

/**
 * What the customer asks for: a product and how many units. Price and name are never taken from here.
 * Validated on construction so a bad line is rejected before any stock is touched.
 */
public record OrderLine(Long productId, int quantity) {

    public OrderLine {
        if (productId == null) {
            throw new DomainValidationException("productId is required");
        }
        if (quantity < 1) {
            throw new DomainValidationException("quantity must be at least 1");
        }
    }
}
