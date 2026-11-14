package com.blossom.ecommerce.product;

import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductCategory;
import com.blossom.ecommerce.product.domain.port.in.ProductCommand;

import java.math.BigDecimal;

public final class ProductFixtures {

    private ProductFixtures() {
    }

    public static Product existingProduct(Long id) {
        return Product.restore(id, "Desk Lamp", "LED lamp", new BigDecimal("45.00"), 10,
                ProductCategory.HOME, true);
    }

    public static ProductCommand validCommand() {
        return new ProductCommand("Desk Lamp", "LED lamp", new BigDecimal("45.00"), 10, ProductCategory.HOME);
    }
}
