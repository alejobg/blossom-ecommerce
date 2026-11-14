package com.blossom.ecommerce.order;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderFixtures {

    public static final Long CUSTOMER_ID = 7L;
    public static final Instant CREATED_AT = Instant.parse("2026-11-14T10:00:00Z");

    private OrderFixtures() {
    }

    public static OrderItem lampItem() {
        return OrderItem.of(1L, "Desk Lamp", new BigDecimal("45.00"), 2);
    }

    public static OrderItem shirtItem() {
        return OrderItem.of(2L, "Cotton T-Shirt", new BigDecimal("19.99"), 1);
    }

    /** Total: 2 x 45.00 + 1 x 19.99 = 109.99 */
    public static Order existingOrder(Long id) {
        return Order.restore(id, CUSTOMER_ID, List.of(lampItem(), shirtItem()), CREATED_AT);
    }
}
