package com.blossom.ecommerce.product.domain.port.in;

import java.math.BigDecimal;

/**
 * Result of a successful reservation: the units were taken from stock, and the product's
 * current name and price are returned so the caller can freeze them (e.g. in an order line).
 */
public record ReservedStock(Long productId, String productName, BigDecimal unitPrice, int quantity) {
}
