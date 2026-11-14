package com.blossom.ecommerce.order.domain.port.in;

import java.util.List;

/**
 * @param customerId taken from the authenticated principal, never from the request body
 */
public record PlaceOrderCommand(Long customerId, List<OrderLine> lines) {

    public PlaceOrderCommand {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
