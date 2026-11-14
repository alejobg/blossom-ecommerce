package com.blossom.ecommerce.product.domain.port.in;

import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductFilter;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;

import java.util.List;

/**
 * Input port: everything the outside world (REST, other modules) can ask the product module.
 */
public interface ProductUseCase {

    Product create(ProductCommand command);

    Product getById(Long id);

    Product update(Long id, ProductCommand command);

    void delete(Long id);

    PageResult<Product> search(ProductFilter filter, PageQuery pageQuery);

    /**
     * Takes the requested units from stock, all or nothing. Every product must exist, be active
     * and have enough units; otherwise nothing is taken. Must run inside the caller's transaction,
     * which keeps the products locked until it commits.
     *
     * @return one reservation per request, in the same order, with the product's current name and price
     */
    List<ReservedStock> reserveStock(List<StockRequest> requests);
}
