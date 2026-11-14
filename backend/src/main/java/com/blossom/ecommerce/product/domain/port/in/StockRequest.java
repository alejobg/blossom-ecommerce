package com.blossom.ecommerce.product.domain.port.in;

/**
 * Units of one product that another module (orders) wants to take from stock.
 */
public record StockRequest(Long productId, int quantity) {
}
