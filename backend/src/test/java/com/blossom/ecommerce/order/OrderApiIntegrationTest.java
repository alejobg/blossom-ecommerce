package com.blossom.ecommerce.order;

import com.blossom.ecommerce.TestcontainersConfiguration;
import com.blossom.ecommerce.order.domain.port.in.OrderLine;
import com.blossom.ecommerce.order.domain.port.in.OrderUseCase;
import com.blossom.ecommerce.order.domain.port.in.PlaceOrderCommand;
import com.blossom.ecommerce.shared.domain.InsufficientStockException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end on real PostgreSQL: sign-up, login, placing orders, history, rollback and concurrency.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    private static final String ORDERS = "/api/v1/orders";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderUseCase orderUseCase;

    private Long lampId;
    private Long shirtId;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        lampId = insertProduct("Desk Lamp", "45.00", 5);
        shirtId = insertProduct("Cotton T-Shirt", "19.99", 1);
    }

    /** Other test classes delete customers and products, so no order may outlive this class. */
    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE orders, order_items RESTART IDENTITY");
        jdbcTemplate.execute("TRUNCATE TABLE products RESTART IDENTITY CASCADE");
        jdbcTemplate.update("DELETE FROM users WHERE role = 'CUSTOMER'");
    }

    @Test
    void registerLoginPlaceOrderAndBrowseHistory() throws Exception {
        String token = registerAndLogin("ana@mail.com");

        JsonNode created = readJson(placeOrder(token, """
                {"items": [{"productId": %d, "quantity": 2}, {"productId": %d, "quantity": 1}]}
                """.formatted(lampId, shirtId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(109.99))
                .andExpect(jsonPath("$.items[0].productName").value("Desk Lamp"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(45.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(90.00))
                .andExpect(jsonPath("$.createdAt").exists()));
        long orderId = created.get("id").asLong();

        assertThat(stockOf(lampId)).isEqualTo(3);
        assertThat(stockOf(shirtId)).isZero();

        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(orderId))
                .andExpect(jsonPath("$.content[0].items.length()").value(2));

        mockMvc.perform(get(ORDERS + "/" + orderId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(109.99));
    }

    @Test
    void historyIsNewestFirst() throws Exception {
        String token = registerAndLogin("ana@mail.com");
        long first = placeOne(token, lampId);
        long second = placeOne(token, shirtId);

        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.content[0].id").value(second))
                .andExpect(jsonPath("$.content[1].id").value(first));
    }

    @Test
    void failedOrderTakesNoStockAndIsNotSaved() throws Exception {
        String token = registerAndLogin("ana@mail.com");

        // the lamp line alone would succeed; the shirt line fails, so the whole order must roll back
        placeOrder(token, """
                {"items": [{"productId": %d, "quantity": 2}, {"productId": %d, "quantity": 3}]}
                """.formatted(lampId, shirtId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.productId").value(shirtId))
                .andExpect(jsonPath("$.requested").value(3))
                .andExpect(jsonPath("$.available").value(1));

        assertThat(stockOf(lampId)).isEqualTo(5);
        assertThat(stockOf(shirtId)).isEqualTo(1);
        assertThat(orderCount()).isZero();
    }

    @Test
    void orderWithUnknownProductReturns404AndTakesNoStock() throws Exception {
        String token = registerAndLogin("ana@mail.com");

        placeOrder(token, """
                {"items": [{"productId": %d, "quantity": 1}, {"productId": 9999, "quantity": 1}]}
                """.formatted(lampId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product with id 9999 was not found"))
                .andExpect(jsonPath("$.productId").value(9999));

        assertThat(stockOf(lampId)).isEqualTo(5);
        assertThat(orderCount()).isZero();
    }

    @Test
    void inactiveProductCannotBeOrdered() throws Exception {
        String token = registerAndLogin("ana@mail.com");
        jdbcTemplate.update("UPDATE products SET active = FALSE WHERE id = ?", lampId);

        placeOrder(token, """
                {"items": [{"productId": %d, "quantity": 1}]}
                """.formatted(lampId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.productId").value(lampId.intValue()));
    }

    @Test
    void repeatedProductIsRejectedWith400() throws Exception {
        String token = registerAndLogin("ana@mail.com");

        placeOrder(token, """
                {"items": [{"productId": %d, "quantity": 1}, {"productId": %d, "quantity": 1}]}
                """.formatted(lampId, lampId))
                .andExpect(status().isBadRequest());

        assertThat(stockOf(lampId)).isEqualTo(5);
    }

    @Test
    void pastOrdersKeepThePriceAndNamePaidAtPurchaseTime() throws Exception {
        String token = registerAndLogin("ana@mail.com");
        long orderId = placeOne(token, lampId);

        jdbcTemplate.update("UPDATE products SET name = 'Renamed', price = 99.00 WHERE id = ?", lampId);

        mockMvc.perform(get(ORDERS + "/" + orderId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[0].productName").value("Desk Lamp"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(45.00))
                .andExpect(jsonPath("$.total").value(45.00));
    }

    @Test
    void customersCannotSeeEachOthersOrders() throws Exception {
        String ana = registerAndLogin("ana@mail.com");
        String bob = registerAndLogin("bob@mail.com");
        long anaOrder = placeOne(ana, lampId);

        mockMvc.perform(get(ORDERS + "/" + anaOrder).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void initialAdminCannotPlaceOrders() throws Exception {
        String adminToken = login("admin@blossom.com", "Admin123!");

        placeOrder(adminToken, """
                {"items": [{"productId": %d, "quantity": 1}]}
                """.formatted(lampId))
                .andExpect(status().isForbidden());
    }

    @Test
    void concurrentPurchasesNeverOversell() throws Exception {
        Long customerId = registerAndGetId("ana@mail.com");
        int buyers = 8;
        // 5 lamps in stock, 8 buyers of one lamp each: exactly 5 must succeed
        List<Callable<Boolean>> purchases = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            purchases.add(() -> {
                try {
                    orderUseCase.place(new PlaceOrderCommand(customerId, List.of(new OrderLine(lampId, 1))));
                    return true;
                } catch (InsufficientStockException e) {
                    return false;
                }
            });
        }

        List<Boolean> results = runConcurrently(purchases);

        assertThat(results).filteredOn(Boolean::booleanValue).hasSize(5);
        assertThat(stockOf(lampId)).isZero();
        assertThat(orderCount()).isEqualTo(5);
    }

    @Test
    void concurrentOrdersWithProductsInOppositeOrderDoNotDeadlock() throws Exception {
        Long customerId = registerAndGetId("ana@mail.com");
        jdbcTemplate.update("UPDATE products SET stock = 100");
        List<Callable<Boolean>> purchases = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            List<OrderLine> lines = i % 2 == 0
                    ? List.of(new OrderLine(lampId, 1), new OrderLine(shirtId, 1))
                    : List.of(new OrderLine(shirtId, 1), new OrderLine(lampId, 1));
            purchases.add(() -> {
                orderUseCase.place(new PlaceOrderCommand(customerId, lines));
                return true;
            });
        }

        assertThat(runConcurrently(purchases)).hasSize(8).containsOnly(true);
        assertThat(stockOf(lampId)).isEqualTo(92);
        assertThat(stockOf(shirtId)).isEqualTo(92);
    }

    /** Starts every task at the same moment; any unexpected exception (e.g. a deadlock) fails the test. */
    private static List<Boolean> runConcurrently(List<Callable<Boolean>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> task : tasks) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private long placeOne(String token, Long productId) throws Exception {
        return readJson(placeOrder(token, """
                {"items": [{"productId": %d, "quantity": 1}]}
                """.formatted(productId)).andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions placeOrder(String token, String body) throws Exception {
        return mockMvc.perform(post(ORDERS).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Long registerAndGetId(String email) throws Exception {
        return readJson(register(email)).get("id").asLong();
    }

    private String registerAndLogin(String email) throws Exception {
        register(email);
        return login(email, "secret123");
    }

    private ResultActions register(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Customer", "email": "%s", "password": "secret123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        JsonNode response = readJson(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk()));
        return "Bearer " + response.get("accessToken").asText();
    }

    private JsonNode readJson(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private Long insertProduct(String name, String price, int stock) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO products (name, price, stock, category, active, created_at, updated_at)
                VALUES (?, ?, ?, 'HOME', TRUE, NOW(), NOW()) RETURNING id
                """, Long.class, name, new BigDecimal(price), stock);
    }

    private int stockOf(Long productId) {
        return jdbcTemplate.queryForObject("SELECT stock FROM products WHERE id = ?", Integer.class, productId);
    }

    private int orderCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
    }
}
