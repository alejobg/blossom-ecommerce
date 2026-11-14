package com.blossom.ecommerce.product.infrastructure.in.rest;

import com.blossom.ecommerce.product.domain.ProductCategory;
import com.blossom.ecommerce.product.domain.ProductFilter;
import com.blossom.ecommerce.product.domain.port.in.ProductCommand;
import com.blossom.ecommerce.product.domain.port.in.ProductUseCase;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import com.blossom.ecommerce.shared.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static com.blossom.ecommerce.product.ProductFixtures.existingProduct;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice: HTTP contract, validation and error mapping, with the use case mocked.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    private static final String VALID_BODY = """
            {"name": "Desk Lamp", "description": "LED lamp", "price": 45.00, "stock": 10, "category": "HOME"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductUseCase productUseCase;

    @Test
    void searchPassesFiltersAndPaginationToUseCase() throws Exception {
        ProductFilter expectedFilter = new ProductFilter("lamp", ProductCategory.HOME,
                new BigDecimal("10"), new BigDecimal("50"));
        when(productUseCase.search(expectedFilter, new PageQuery(1, 5)))
                .thenReturn(new PageResult<>(List.of(existingProduct(1L)), 1, 5, 6, 2));

        mockMvc.perform(get("/api/v1/products")
                        .param("name", "lamp")
                        .param("category", "HOME")
                        .param("minPrice", "10")
                        .param("maxPrice", "50")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Desk Lamp"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void searchRejectsInvalidPriceRange() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("minPrice", "50").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("minPrice must be less than or equal to maxPrice"));

        verifyNoInteractions(productUseCase);
    }

    @Test
    void searchRejectsUnknownCategory() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("category", "CARS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value 'CARS' for parameter 'category'"));
    }

    @Test
    void searchRejectsTooLargePageSize() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturnsProduct() throws Exception {
        when(productUseCase.getById(1L)).thenReturn(existingProduct(1L));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.price").value(45.00))
                .andExpect(jsonPath("$.category").value("HOME"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(productUseCase.getById(99L)).thenThrow(new ResourceNotFoundException("Product", 99L));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Product with id 99 was not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns201WithLocation() throws Exception {
        when(productUseCase.create(any(ProductCommand.class))).thenReturn(existingProduct(7L));

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/products/7"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturnsFieldErrorsWhenBodyIsInvalid() throws Exception {
        String invalidBody = """
                {"name": "", "price": 0, "stock": -1}
                """;

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stock").exists())
                .andExpect(jsonPath("$.errors.category").exists());

        verify(productUseCase, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReturns400WhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturnsUpdatedProduct() throws Exception {
        when(productUseCase.update(eq(1L), any(ProductCommand.class))).thenReturn(existingProduct(1L));

        mockMvc.perform(put("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());

        verify(productUseCase).delete(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Product", 99L)).when(productUseCase).delete(99L);

        mockMvc.perform(delete("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void writesWithoutTokenReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productUseCase);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void writesAsCustomerReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productUseCase);
    }

    @Test
    void unexpectedErrorsReturn500WithoutLeakingDetails() throws Exception {
        when(productUseCase.getById(1L)).thenThrow(new IllegalStateException("db password is 1234"));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }
}
