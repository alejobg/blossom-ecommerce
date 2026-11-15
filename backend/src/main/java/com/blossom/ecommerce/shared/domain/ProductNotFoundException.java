package com.blossom.ecommerce.shared.domain;

/**
 * A purchase references a product that does not exist or is no longer active. Raised by the product
 * module and seen by the order module, so it lives in shared. The id lets the API tell the client
 * which item failed without parsing the message.
 */
public class ProductNotFoundException extends ResourceNotFoundException {

    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("Product", productId);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
