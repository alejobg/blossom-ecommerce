package com.blossom.ecommerce.order.infrastructure.in.rest;

import com.blossom.ecommerce.order.domain.port.in.OrderLine;
import com.blossom.ecommerce.order.domain.port.in.OrderUseCase;
import com.blossom.ecommerce.order.domain.port.in.PlaceOrderCommand;
import com.blossom.ecommerce.shared.domain.InsufficientStockException;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import com.blossom.ecommerce.shared.security.JwtService;
import com.blossom.ecommerce.shared.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.blossom.ecommerce.order.OrderFixtures.CUSTOMER_ID;
import static com.blossom.ecommerce.order.OrderFixtures.existingOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice with real tokens from {@link JwtService}, so role checks and the principal are exercised.
 */
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    private static final String ORDERS = "/api/v1/orders";
    private static final String VALID_BODY = """
            {"items": [{"productId": 1, "quantity": 2}, {"productId": 2, "quantity": 1}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private OrderUseCase orderUseCase;

    @Test
    void placeReturns201WithLocationAndComputedTotal() throws Exception {
        PlaceOrderCommand expected = new PlaceOrderCommand(CUSTOMER_ID,
                List.of(new OrderLine(1L, 2), new OrderLine(2L, 1)));
        when(orderUseCase.place(expected)).thenReturn(existingOrder(10L));

        mockMvc.perform(post(ORDERS).header(HttpHeaders.AUTHORIZATION, customerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/orders/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.total").value(109.99))
                .andExpect(jsonPath("$.items[0].productName").value("Desk Lamp"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(45.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(90.00))
                .andExpect(jsonPath("$.createdAt").value("2026-11-14T10:00:00Z"));
    }

    @Test
    void placeTakesTheCustomerFromTheTokenAndIgnoresClientTotalsAndIds() throws Exception {
        String body = """
                {"customerId": 999, "total": 0.01, "items": [{"productId": 1, "quantity": 2, "unitPrice": 0.01}]}
                """;
        when(orderUseCase.place(any())).thenReturn(existingOrder(10L));

        mockMvc.perform(post(ORDERS).header(HttpHeaders.AUTHORIZATION, customerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        verify(orderUseCase).place(new PlaceOrderCommand(CUSTOMER_ID, List.of(new OrderLine(1L, 2))));
    }

    @Test
    void placeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));

        verifyNoInteractions(orderUseCase);
    }

    @Test
    void placeAsAdminReturns403() throws Exception {
        mockMvc.perform(post(ORDERS).header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));

        verifyNoInteractions(orderUseCase);
    }

    @Test
    void historyAsAdminReturns403() throws Exception {
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void placeReturns400WhenItemsAreMissingOrEmpty() throws Exception {
        for (String body : List.of("{}", """
                {"items": []}
                """)) {
            mockMvc.perform(post(ORDERS).header(HttpHeaders.AUTHORIZATION, customerToken())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.items").exists());
        }
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void placeReturns400WithFieldErrorsForInvalidLines() throws Exception {
        String body = """
                {"items": [{"productId": null, "quantity": 0}]}
                """;

        mockMvc.perform(post(ORDERS).header(HttpHeaders.AUTHORIZATION, customerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].productId']").exists())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());

        verifyNoInteractions(orderUseCase);
    }

    @Test
    void placeReturns409WithProductDetailWhenStockIsInsufficient() throws Exception {
        when(orderUseCase.place(any())).thenThrow(new InsufficientStockException(1L, "Desk Lamp", 2, 1));

        mockMvc.perform(post(ORDERS).header(HttpHeaders.AUTHORIZATION, customerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.detail")
                        .value("Insufficient stock for product 'Desk Lamp' (id 1): requested 2, available 1"))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.requested").value(2))
                .andExpect(jsonPath("$.available").value(1));
    }

    @Test
    void placeReturns404WhenAProductDoesNotExist() throws Exception {
        when(orderUseCase.place(any())).thenThrow(new ResourceNotFoundException("Product", 1L));

        mockMvc.perform(post(ORDERS).header(HttpHeaders.AUTHORIZATION, customerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyReturnsTheCustomersOrdersPaginated() throws Exception {
        when(orderUseCase.listForCustomer(CUSTOMER_ID, new PageQuery(1, 5)))
                .thenReturn(new PageResult<>(List.of(existingOrder(3L)), 1, 5, 6, 2));

        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, customerToken())
                        .param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.content[0].items.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void historyWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(ORDERS)).andExpect(status().isUnauthorized());
    }

    @Test
    void getByIdReturnsTheOrder() throws Exception {
        when(orderUseCase.getForCustomer(CUSTOMER_ID, 3L)).thenReturn(existingOrder(3L));

        mockMvc.perform(get(ORDERS + "/3").header(HttpHeaders.AUTHORIZATION, customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.total").value(109.99));
    }

    @Test
    void getByIdReturns404ForAnotherCustomersOrder() throws Exception {
        when(orderUseCase.getForCustomer(CUSTOMER_ID, 3L)).thenThrow(new ResourceNotFoundException("Order", 3L));

        mockMvc.perform(get(ORDERS + "/3").header(HttpHeaders.AUTHORIZATION, customerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order with id 3 was not found"));
    }

    private String customerToken() {
        return "Bearer " + jwtService.issue(CUSTOMER_ID, "ana@mail.com", "CUSTOMER");
    }

    private String adminToken() {
        return "Bearer " + jwtService.issue(1L, "admin@blossom.com", "ADMIN");
    }
}
