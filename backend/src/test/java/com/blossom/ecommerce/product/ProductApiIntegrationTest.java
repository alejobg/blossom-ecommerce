package com.blossom.ecommerce.product;

import com.blossom.ecommerce.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through every layer: HTTP -> use case -> JPA -> PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ProductApiIntegrationTest {

    private static final String PRODUCTS = "/api/v1/products";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("TRUNCATE TABLE products RESTART IDENTITY CASCADE");
        adminToken = "Bearer " + loginAsInitialAdmin();
    }

    @Test
    void catalogIsReadableWithoutToken() throws Exception {
        long id = createProduct("Desk Lamp", "45.00", "HOME");

        mockMvc.perform(get(PRODUCTS)).andExpect(status().isOk());
        mockMvc.perform(get(PRODUCTS + "/" + id)).andExpect(status().isOk());
    }

    @Test
    void fullProductLifecycle() throws Exception {
        long id = createProduct("Desk Lamp", "45.00", "HOME");

        mockMvc.perform(get(PRODUCTS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Desk Lamp"));

        mockMvc.perform(put(PRODUCTS + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Floor Lamp", "80.00", "HOME")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Floor Lamp"))
                .andExpect(jsonPath("$.price").value(80.00));

        mockMvc.perform(delete(PRODUCTS + "/" + id).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PRODUCTS + "/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchCombinesFilters() throws Exception {
        createProduct("Desk Lamp", "45.00", "HOME");
        createProduct("Lava Lamp", "120.00", "HOME");
        createProduct("Lamp Charger", "30.00", "ELECTRONICS");

        mockMvc.perform(get(PRODUCTS)
                        .param("name", "lamp")
                        .param("category", "HOME")
                        .param("maxPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Desk Lamp"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void invalidProductIsRejectedAndNotPersisted() throws Exception {
        mockMvc.perform(post(PRODUCTS)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "-5", "HOME")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(PRODUCTS))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private long createProduct(String name, String price, String category) throws Exception {
        String response = mockMvc.perform(post(PRODUCTS)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, price, category)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    private String loginAsInitialAdmin() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@blossom.com", "password": "Admin123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private static String body(String name, String price, String category) {
        return """
                {"name": "%s", "description": "test product", "price": %s, "stock": 10, "category": "%s"}
                """.formatted(name, price, category);
    }
}
