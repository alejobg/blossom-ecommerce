package com.blossom.ecommerce.order.infrastructure.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, List<OrderItemResponse> items, BigDecimal total, Instant createdAt) {

    public record OrderItemResponse(Long productId, String productName, BigDecimal unitPrice, int quantity,
                                    BigDecimal subtotal) {
    }
}
