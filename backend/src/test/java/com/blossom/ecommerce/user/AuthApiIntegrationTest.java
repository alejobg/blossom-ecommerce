package com.blossom.ecommerce.user;

import com.blossom.ecommerce.TestcontainersConfiguration;
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
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end: sign-up, login, authenticated request and role-based access, on real PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // keep the admin created by the migration
        jdbcTemplate.update("DELETE FROM users WHERE role = 'CUSTOMER'");
    }

    @Test
    void registerLoginAndFetchProfile() throws Exception {
        register("Ana Perez", "Ana@Mail.com", "secret123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@mail.com"));

        String token = login("ana@mail.com", "secret123");

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Perez"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        register("Ana Perez", "ana@mail.com", "secret123").andExpect(status().isCreated());

        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = 'ana@mail.com'", String.class);
        assertThat(hash).startsWith("$2a$").doesNotContain("secret123");
    }

    @Test
    void emailIsUniqueIgnoringCase() throws Exception {
        register("Ana Perez", "ana@mail.com", "secret123").andExpect(status().isCreated());

        register("Ana Again", "ANA@mail.com", "secret456").andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        register("Ana Perez", "ana@mail.com", "secret123").andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("ana@mail.com", "secret999")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotManageTheCatalog() throws Exception {
        register("Ana Perez", "ana@mail.com", "secret123").andExpect(status().isCreated());
        String token = login("ana@mail.com", "secret123");

        mockMvc.perform(post("/api/v1/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Lamp", "price": 10, "stock": 1, "category": "HOME"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void initialAdminCanLogIn() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("admin@blossom.com", "Admin123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void corsPreflightFromTheFrontendIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    private ResultActions register(String name, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "email": "%s", "password": "%s"}
                        """.formatted(name, email, password)));
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private static String credentials(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
