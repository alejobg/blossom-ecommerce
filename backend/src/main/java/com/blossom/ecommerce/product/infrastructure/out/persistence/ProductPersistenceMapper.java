package com.blossom.ecommerce.product.infrastructure.out.persistence;

import com.blossom.ecommerce.product.domain.Product;

final class ProductPersistenceMapper {

    private ProductPersistenceMapper() {
    }

    static Product toDomain(ProductEntity entity) {
        return Product.restore(entity.getId(), entity.getName(), entity.getDescription(), entity.getPrice(),
                entity.getStock(), entity.getCategory(), entity.isActive());
    }

    static ProductEntity toNewEntity(Product product) {
        return new ProductEntity(product.getName(), product.getDescription(), product.getPrice(),
                product.getStock(), product.getCategory(), product.isActive());
    }

    static void copyToEntity(Product product, ProductEntity entity) {
        entity.apply(product.getName(), product.getDescription(), product.getPrice(),
                product.getStock(), product.getCategory(), product.isActive());
    }
}
