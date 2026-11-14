package com.blossom.ecommerce.product.domain.port.out;

import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductFilter;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;

import java.util.Optional;

/**
 * Output port: what the product module needs from persistence, expressed in domain terms.
 */
public interface ProductRepositoryPort {

    Product save(Product product);

    /** Returns only active products; deactivated ones behave as missing. */
    Optional<Product> findActiveById(Long id);

    PageResult<Product> searchActive(ProductFilter filter, PageQuery pageQuery);
}
