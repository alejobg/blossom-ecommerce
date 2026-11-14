package com.blossom.ecommerce.product.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Product aggregate. Plain Java: no JPA or Spring annotations, so business rules
 * can be unit tested without any framework.
 */
public class Product {

    static final int MAX_NAME_LENGTH = 120;
    static final int MAX_DESCRIPTION_LENGTH = 2000;

    private final Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private ProductCategory category;
    private boolean active;

    private Product(Long id, String name, String description, BigDecimal price, int stock,
                    ProductCategory category, boolean active) {
        this.id = id;
        this.active = active;
        applyDetails(name, description, price, stock, category);
    }

    /** Creates a new, not yet persisted, active product. */
    public static Product create(String name, String description, BigDecimal price, int stock,
                                 ProductCategory category) {
        return new Product(null, name, description, price, stock, category, true);
    }

    /** Rebuilds a product that already exists (used by the persistence adapter). */
    public static Product restore(Long id, String name, String description, BigDecimal price, int stock,
                                  ProductCategory category, boolean active) {
        Objects.requireNonNull(id, "id is required to restore a product");
        return new Product(id, name, description, price, stock, category, active);
    }

    public void update(String name, String description, BigDecimal price, int stock, ProductCategory category) {
        applyDetails(name, description, price, stock, category);
    }

    /** Soft delete: the product disappears from the catalog but past orders keep their reference. */
    public void deactivate() {
        this.active = false;
    }

    public boolean hasStock(int quantity) {
        return quantity > 0 && stock >= quantity;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new DomainValidationException("quantity must be greater than 0");
        }
        if (stock < quantity) {
            throw new DomainValidationException(
                    "Insufficient stock for product '%s': requested %d, available %d".formatted(name, quantity, stock));
        }
        stock -= quantity;
    }

    private void applyDetails(String name, String description, BigDecimal price, int stock,
                              ProductCategory category) {
        this.name = requireValidName(name);
        this.description = requireValidDescription(description);
        this.price = requireValidPrice(price);
        this.stock = requireValidStock(stock);
        this.category = requireCategory(category);
    }

    private static String requireValidName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainValidationException("name must have at most " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    private static String requireValidDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new DomainValidationException(
                    "description must have at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return trimmed;
    }

    private static BigDecimal requireValidPrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new DomainValidationException("price must be greater than 0");
        }
        return price;
    }

    private static int requireValidStock(int stock) {
        if (stock < 0) {
            throw new DomainValidationException("stock must be greater than or equal to 0");
        }
        return stock;
    }

    private static ProductCategory requireCategory(ProductCategory category) {
        if (category == null) {
            throw new DomainValidationException("category is required");
        }
        return category;
    }

    public Long getId() {
        return id;
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
