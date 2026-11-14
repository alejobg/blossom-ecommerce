package com.blossom.ecommerce.product.domain.port.in;

import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductFilter;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;

/**
 * Input port: everything the outside world (REST, other modules) can ask the product module.
 */
public interface ProductUseCase {

    Product create(ProductCommand command);

    Product getById(Long id);

    Product update(Long id, ProductCommand command);

    void delete(Long id);

    PageResult<Product> search(ProductFilter filter, PageQuery pageQuery);
}
