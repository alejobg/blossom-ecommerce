package com.blossom.ecommerce.order.infrastructure.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Only products and quantities: the customer comes from the token and prices from the catalog.
 */
public record PlaceOrderRequest(
        @NotEmpty @Size(max = MAX_ITEMS) List<@NotNull @Valid OrderItemRequest> items) {

    public static final int MAX_ITEMS = 50;
}
