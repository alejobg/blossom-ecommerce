package com.blossom.ecommerce.order.domain.port.in;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;

/**
 * Input port of the order module.
 */
public interface OrderUseCase {

    /** Reserves stock and saves the order atomically: either both happen or neither does. */
    Order place(PlaceOrderCommand command);

    /** The customer's orders, most recent first. */
    PageResult<Order> listForCustomer(Long customerId, PageQuery pageQuery);

    /** Fails with not found when the order does not exist or belongs to someone else. */
    Order getForCustomer(Long customerId, Long orderId);
}
