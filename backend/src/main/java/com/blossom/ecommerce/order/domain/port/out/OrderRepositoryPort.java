package com.blossom.ecommerce.order.domain.port.out;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;

import java.util.Optional;

public interface OrderRepositoryPort {

    /** Persists a new order. Orders are immutable, so there is no update. */
    Order save(Order order);

    /** Most recent first, with their items loaded. */
    PageResult<Order> findByCustomerId(Long customerId, PageQuery pageQuery);

    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
}
