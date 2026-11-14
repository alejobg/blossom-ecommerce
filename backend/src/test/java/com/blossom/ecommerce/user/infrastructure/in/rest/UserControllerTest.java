package com.blossom.ecommerce.user.infrastructure.in.rest;

import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import com.blossom.ecommerce.shared.security.JwtService;
import com.blossom.ecommerce.shared.security.SecurityConfig;
import com.blossom.ecommerce.user.domain.port.in.UserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.blossom.ecommerce.user.UserFixtures.existingCustomer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uses real tokens from {@link JwtService}, so the JWT filter is exercised end to end at the web layer.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final String ME = "/api/v1/users/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserUseCase userUseCase;

    @Test
    void meReturnsTheUserIdentifiedByTheToken() throws Exception {
        when(userUseCase.getById(1L)).thenReturn(existingCustomer(1L));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ana Perez"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void meWithoutTokenReturns401ProblemDetail() throws Exception {
        mockMvc.perform(get(ME))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.instance").value(ME));

        verifyNoInteractions(userUseCase);
    }

    @Test
    void meWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithNonBearerSchemeReturns401() throws Exception {
        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Basic YW5hOnNlY3JldA=="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturns404WhenTheAccountNoLongerExists() throws Exception {
        when(userUseCase.getById(5L)).thenThrow(new ResourceNotFoundException("User", 5L));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(5L)))
                .andExpect(status().isNotFound());
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtService.issue(userId, "ana@mail.com", "CUSTOMER");
    }
}
