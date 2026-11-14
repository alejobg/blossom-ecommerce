package com.blossom.ecommerce.product.infrastructure.out.persistence;

import com.blossom.ecommerce.product.domain.ProductCategory;
import com.blossom.ecommerce.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class ProductEntity extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCategory category;

    @Column(nullable = false)
    private boolean active;

    protected ProductEntity() {
        // required by JPA
    }

    ProductEntity(String name, String description, BigDecimal price, int stock,
                  ProductCategory category, boolean active) {
        apply(name, description, price, stock, category, active);
    }

    void apply(String name, String description, BigDecimal price, int stock,
               ProductCategory category, boolean active) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }
}
