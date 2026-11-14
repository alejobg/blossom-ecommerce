package com.blossom.ecommerce.order.infrastructure.in.rest;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.port.in.OrderUseCase;
import com.blossom.ecommerce.order.infrastructure.in.rest.dto.OrderResponse;
import com.blossom.ecommerce.order.infrastructure.in.rest.dto.PlaceOrderRequest;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.security.AuthenticatedUser;
import com.blossom.ecommerce.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Every operation acts on the authenticated customer's own orders: the customer id always
 * comes from the token. Access is restricted to CUSTOMER in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Place orders and browse the purchase history")
public class OrderController {

    private final OrderUseCase orderUseCase;

    public OrderController(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @PostMapping
    @Operation(summary = "Place an order; stock is reserved and prices are frozen at purchase time")
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @Valid @RequestBody PlaceOrderRequest request) {
        Order placed = orderUseCase.place(OrderRestMapper.toCommand(principal.id(), request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(placed.getId())
                .toUri();
        return ResponseEntity.created(location).body(OrderRestMapper.toResponse(placed));
    }

    @GetMapping
    @Operation(summary = "The authenticated customer's orders, most recent first")
    public PageResponse<OrderResponse> history(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(orderUseCase.listForCustomer(principal.id(), new PageQuery(page, size)),
                OrderRestMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Order detail (404 if it belongs to another customer)")
    public OrderResponse getById(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        return OrderRestMapper.toResponse(orderUseCase.getForCustomer(principal.id(), id));
    }
}
