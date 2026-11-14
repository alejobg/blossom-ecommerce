package com.blossom.ecommerce.order.infrastructure.out.persistence;

import com.blossom.ecommerce.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The customer is kept as a plain id: the order module does not map the user module's entities.
 */
@Entity
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

    @Column(name = "customer_id", nullable = false, updatable = false)
    private Long customerId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItemEntity> items = new ArrayList<>();

    protected OrderEntity() {
        // required by JPA
    }

    OrderEntity(Long customerId, BigDecimal total) {
        this.customerId = customerId;
        this.total = total;
    }

    /** Keeps both sides of the association in sync. */
    void addItem(OrderItemEntity item) {
        items.add(item);
        item.attachTo(this);
    }

    public Long getCustomerId() {
        return customerId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }
}
