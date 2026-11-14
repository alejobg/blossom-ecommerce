package com.blossom.ecommerce.product.infrastructure.in.rest;

import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.port.in.ProductCommand;
import com.blossom.ecommerce.product.infrastructure.in.rest.dto.ProductRequest;
import com.blossom.ecommerce.product.infrastructure.in.rest.dto.ProductResponse;

final class ProductRestMapper {

    private ProductRestMapper() {
    }

    static ProductCommand toCommand(ProductRequest request) {
        return new ProductCommand(request.name(), request.description(), request.price(),
                request.stock(), request.category());
    }

    static ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStock(), product.getCategory());
    }
}
