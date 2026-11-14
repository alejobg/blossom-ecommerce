package com.blossom.ecommerce.order.application;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.port.in.OrderLine;
import com.blossom.ecommerce.order.domain.port.in.PlaceOrderCommand;
import com.blossom.ecommerce.order.domain.port.out.OrderRepositoryPort;
import com.blossom.ecommerce.product.domain.port.in.ProductUseCase;
import com.blossom.ecommerce.product.domain.port.in.ReservedStock;
import com.blossom.ecommerce.product.domain.port.in.StockRequest;
import com.blossom.ecommerce.shared.domain.DomainValidationException;
import com.blossom.ecommerce.shared.domain.InsufficientStockException;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.blossom.ecommerce.order.OrderFixtures.CREATED_AT;
import static com.blossom.ecommerce.order.OrderFixtures.CUSTOMER_ID;
import static com.blossom.ecommerce.order.OrderFixtures.existingOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private ProductUseCase productUseCase;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeReservesStockAndSavesOrderWithFrozenPricesAndComputedTotal() {
        when(productUseCase.reserveStock(List.of(new StockRequest(1L, 2), new StockRequest(2L, 1))))
                .thenReturn(List.of(
                        new ReservedStock(1L, "Desk Lamp", new BigDecimal("45.00"), 2),
                        new ReservedStock(2L, "Cotton T-Shirt", new BigDecimal("19.99"), 1)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order toSave = inv.getArgument(0);
            return Order.restore(10L, toSave.getCustomerId(), toSave.getItems(), CREATED_AT);
        });

        Order placed = orderService.place(new PlaceOrderCommand(CUSTOMER_ID,
                List.of(new OrderLine(1L, 2), new OrderLine(2L, 1))));

        assertThat(placed.getId()).isEqualTo(10L);
        assertThat(placed.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(placed.getTotal()).isEqualByComparingTo("109.99");
        assertThat(placed.getItems()).extracting(item -> item.getProductName())
                .containsExactly("Desk Lamp", "Cotton T-Shirt");
        assertThat(placed.getItems().get(0).getUnitPrice()).isEqualByComparingTo("45.00");
    }

    @Test
    void placeFailsAndSavesNothingWhenStockIsInsufficient() {
        when(productUseCase.reserveStock(List.of(new StockRequest(1L, 5))))
                .thenThrow(new InsufficientStockException(1L, "Desk Lamp", 5, 2));

        assertThatThrownBy(() -> orderService.place(new PlaceOrderCommand(CUSTOMER_ID, List.of(new OrderLine(1L, 5)))))
                .isInstanceOf(InsufficientStockException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeFailsAndSavesNothingWhenProductDoesNotExist() {
        when(productUseCase.reserveStock(List.of(new StockRequest(99L, 1))))
                .thenThrow(new ResourceNotFoundException("Product", 99L));

        assertThatThrownBy(() -> orderService.place(new PlaceOrderCommand(CUSTOMER_ID, List.of(new OrderLine(99L, 1)))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product with id 99 was not found");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeRejectsDuplicatedProductsBeforeTouchingStock() {
        PlaceOrderCommand command = new PlaceOrderCommand(CUSTOMER_ID,
                List.of(new OrderLine(1L, 1), new OrderLine(1L, 2)));

        assertThatThrownBy(() -> orderService.place(command)).isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(productUseCase, orderRepository);
    }

    @Test
    void placeRejectsEmptyOrderBeforeTouchingStock() {
        assertThatThrownBy(() -> orderService.place(new PlaceOrderCommand(CUSTOMER_ID, List.of())))
                .isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(productUseCase, orderRepository);
    }

    @Test
    void listForCustomerDelegatesToRepository() {
        PageQuery pageQuery = new PageQuery(0, 10);
        PageResult<Order> page = new PageResult<>(List.of(existingOrder(1L)), 0, 10, 1, 1);
        when(orderRepository.findByCustomerId(CUSTOMER_ID, pageQuery)).thenReturn(page);

        assertThat(orderService.listForCustomer(CUSTOMER_ID, pageQuery)).isSameAs(page);
    }

    @Test
    void getForCustomerReturnsOwnOrder() {
        when(orderRepository.findByIdAndCustomerId(1L, CUSTOMER_ID)).thenReturn(Optional.of(existingOrder(1L)));

        assertThat(orderService.getForCustomer(CUSTOMER_ID, 1L).getId()).isEqualTo(1L);
    }

    @Test
    void getForCustomerReportsSomeoneElsesOrderAsNotFound() {
        when(orderRepository.findByIdAndCustomerId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getForCustomer(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order with id 1 was not found");
    }
}
