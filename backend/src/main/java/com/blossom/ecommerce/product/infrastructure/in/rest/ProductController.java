package com.blossom.ecommerce.product.infrastructure.in.rest;

import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductCategory;
import com.blossom.ecommerce.product.domain.ProductFilter;
import com.blossom.ecommerce.product.domain.port.in.ProductUseCase;
import com.blossom.ecommerce.product.infrastructure.in.rest.dto.ProductRequest;
import com.blossom.ecommerce.product.infrastructure.in.rest.dto.ProductResponse;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product catalog: CRUD and search")
public class ProductController {

    private final ProductUseCase productUseCase;

    public ProductController(ProductUseCase productUseCase) {
        this.productUseCase = productUseCase;
    }

    @GetMapping
    @Operation(summary = "Search products by name, category and price range (all optional)")
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ProductFilter filter = new ProductFilter(name, category, minPrice, maxPrice);
        return PageResponse.from(productUseCase.search(filter, new PageQuery(page, size)),
                ProductRestMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    public ProductResponse getById(@PathVariable Long id) {
        return ProductRestMapper.toResponse(productUseCase.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        Product created = productUseCase.create(ProductRestMapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(ProductRestMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a product's data")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ProductRestMapper.toResponse(productUseCase.update(id, ProductRestMapper.toCommand(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a product from the catalog (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
