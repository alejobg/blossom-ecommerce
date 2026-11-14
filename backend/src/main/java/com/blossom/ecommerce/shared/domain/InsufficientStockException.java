package com.blossom.ecommerce.shared.domain;

/**
 * A purchase asks for more units than a product has. Raised by the product module and seen by
 * the order module, so it lives in shared. The extra fields let the API tell the client which
 * product failed and how many units are left.
 */
public class InsufficientStockException extends ConflictException {

    private final Long productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(Long productId, String productName, int requested, int available) {
        super("Insufficient stock for product '%s' (id %d): requested %d, available %d"
                .formatted(productName, productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
