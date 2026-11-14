package com.blossom.ecommerce.order.infrastructure.out.persistence;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderItem;

final class OrderPersistenceMapper {

    private OrderPersistenceMapper() {
    }

    static Order toDomain(OrderEntity entity) {
        return Order.restore(entity.getId(), entity.getCustomerId(),
                entity.getItems().stream().map(OrderPersistenceMapper::toDomain).toList(),
                entity.getCreatedAt());
    }

    static OrderEntity toNewEntity(Order order) {
        OrderEntity entity = new OrderEntity(order.getCustomerId(), order.getTotal());
        order.getItems().forEach(item -> entity.addItem(new OrderItemEntity(item.getProductId(),
                item.getProductName(), item.getUnitPrice(), item.getQuantity(), item.getSubtotal())));
        return entity;
    }

    private static OrderItem toDomain(OrderItemEntity entity) {
        return OrderItem.of(entity.getProductId(), entity.getProductName(), entity.getUnitPrice(),
                entity.getQuantity());
    }
}
