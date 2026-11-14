package com.blossom.ecommerce.order.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;

import java.math.BigDecimal;

/**
 * One line of an order. Name and unit price are copies taken at purchase time, so the order
 * keeps showing what the customer paid even if the product is later renamed, repriced or removed.
 */
public final class OrderItem {

    private final Long productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;

    private OrderItem(Long productId, String productName, BigDecimal unitPrice, int quantity) {
        if (productId == null) {
            throw new DomainValidationException("productId is required");
        }
        if (productName == null || productName.isBlank()) {
            throw new DomainValidationException("productName is required");
        }
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new DomainValidationException("unitPrice must be greater than 0");
        }
        if (quantity < 1) {
            throw new DomainValidationException("quantity must be at least 1");
        }
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public static OrderItem of(Long productId, String productName, BigDecimal unitPrice, int quantity) {
        return new OrderItem(productId, productName, unitPrice, quantity);
    }

    /** Always derived, never stored independently from price and quantity. */
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }
}
