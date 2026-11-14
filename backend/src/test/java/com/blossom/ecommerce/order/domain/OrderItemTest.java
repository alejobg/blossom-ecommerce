package com.blossom.ecommerce.order.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    private static final BigDecimal PRICE = new BigDecimal("19.99");

    @Test
    void subtotalIsUnitPriceTimesQuantity() {
        OrderItem item = OrderItem.of(1L, "T-Shirt", PRICE, 3);

        assertThat(item.getSubtotal()).isEqualByComparingTo("59.97");
        assertThat(item.getProductId()).isEqualTo(1L);
        assertThat(item.getProductName()).isEqualTo("T-Shirt");
        assertThat(item.getUnitPrice()).isEqualByComparingTo(PRICE);
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsQuantityBelowOne(int quantity) {
        assertThatThrownBy(() -> OrderItem.of(1L, "T-Shirt", PRICE, quantity))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("quantity must be at least 1");
    }

    @Test
    void rejectsMissingProductId() {
        assertThatThrownBy(() -> OrderItem.of(null, "T-Shirt", PRICE, 1))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsBlankProductName() {
        assertThatThrownBy(() -> OrderItem.of(1L, " ", PRICE, 1))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsNonPositiveUnitPrice() {
        assertThatThrownBy(() -> OrderItem.of(1L, "T-Shirt", BigDecimal.ZERO, 1))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> OrderItem.of(1L, "T-Shirt", null, 1))
                .isInstanceOf(DomainValidationException.class);
    }
}
