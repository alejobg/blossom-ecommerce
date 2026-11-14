package com.blossom.ecommerce.order.infrastructure.in.rest;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderItem;
import com.blossom.ecommerce.order.domain.port.in.OrderLine;
import com.blossom.ecommerce.order.domain.port.in.PlaceOrderCommand;
import com.blossom.ecommerce.order.infrastructure.in.rest.dto.OrderResponse;
import com.blossom.ecommerce.order.infrastructure.in.rest.dto.OrderResponse.OrderItemResponse;
import com.blossom.ecommerce.order.infrastructure.in.rest.dto.PlaceOrderRequest;

final class OrderRestMapper {

    private OrderRestMapper() {
    }

    static PlaceOrderCommand toCommand(Long customerId, PlaceOrderRequest request) {
        return new PlaceOrderCommand(customerId, request.items().stream()
                .map(item -> new OrderLine(item.productId(), item.quantity()))
                .toList());
    }

    static OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(),
                order.getItems().stream().map(OrderRestMapper::toResponse).toList(),
                order.getTotal(),
                order.getCreatedAt());
    }

    private static OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(item.getProductId(), item.getProductName(), item.getUnitPrice(),
                item.getQuantity(), item.getSubtotal());
    }
}
