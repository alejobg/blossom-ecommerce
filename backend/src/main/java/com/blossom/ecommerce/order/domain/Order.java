package com.blossom.ecommerce.order.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Order aggregate: a customer's purchase. Immutable once placed. The total is always computed
 * here from the items, never taken from the client.
 */
public final class Order {

    private final Long id;
    private final Long customerId;
    private final List<OrderItem> items;
    private final Instant createdAt;

    private Order(Long id, Long customerId, List<OrderItem> items, Instant createdAt) {
        if (customerId == null) {
            throw new DomainValidationException("customerId is required");
        }
        this.id = id;
        this.customerId = customerId;
        this.items = requireValidItems(items);
        this.createdAt = createdAt;
    }

    /** A new, not yet persisted order (id and creation time are assigned when it is saved). */
    public static Order place(Long customerId, List<OrderItem> items) {
        return new Order(null, customerId, items, null);
    }

    /** Rebuilds an order that already exists (used by the persistence adapter). */
    public static Order restore(Long id, Long customerId, List<OrderItem> items, Instant createdAt) {
        Objects.requireNonNull(id, "id is required to restore an order");
        return new Order(id, customerId, items, createdAt);
    }

    /**
     * Shared with the use case so a request is rejected before any stock is touched.
     * Items themselves already guarantee quantity >= 1.
     */
    public static void requireDistinctProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new DomainValidationException("an order needs at least one item");
        }
        Set<Long> seen = new HashSet<>();
        for (Long productId : productIds) {
            if (!seen.add(productId)) {
                throw new DomainValidationException(
                        "product %s appears more than once; use a single line with the total quantity".formatted(productId));
            }
        }
    }

    private static List<OrderItem> requireValidItems(List<OrderItem> items) {
        if (items == null) {
            throw new DomainValidationException("an order needs at least one item");
        }
        requireDistinctProducts(items.stream().map(OrderItem::getProductId).toList());
        return List.copyOf(items);
    }

    public BigDecimal getTotal() {
        return items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
