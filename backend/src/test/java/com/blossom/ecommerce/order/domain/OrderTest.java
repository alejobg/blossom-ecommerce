package com.blossom.ecommerce.order.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.blossom.ecommerce.order.OrderFixtures.CREATED_AT;
import static com.blossom.ecommerce.order.OrderFixtures.CUSTOMER_ID;
import static com.blossom.ecommerce.order.OrderFixtures.lampItem;
import static com.blossom.ecommerce.order.OrderFixtures.shirtItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void totalIsTheSumOfSubtotals() {
        Order order = Order.place(CUSTOMER_ID, List.of(lampItem(), shirtItem()));

        assertThat(order.getTotal()).isEqualByComparingTo("109.99");
    }

    @Test
    void totalKeepsCentsExact() {
        Order order = Order.place(CUSTOMER_ID, List.of(
                OrderItem.of(1L, "A", new BigDecimal("0.10"), 3),
                OrderItem.of(2L, "B", new BigDecimal("0.20"), 1)));

        assertThat(order.getTotal()).isEqualByComparingTo("0.50");
    }

    @Test
    void placedOrderIsNotPersistedYet() {
        Order order = Order.place(CUSTOMER_ID, List.of(lampItem()));

        assertThat(order.getId()).isNull();
        assertThat(order.getCreatedAt()).isNull();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void restoreKeepsIdentityAndCreationTime() {
        Order order = Order.restore(3L, CUSTOMER_ID, List.of(lampItem()), CREATED_AT);

        assertThat(order.getId()).isEqualTo(3L);
        assertThat(order.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void restoreRequiresId() {
        assertThatThrownBy(() -> Order.restore(null, CUSTOMER_ID, List.of(lampItem()), CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void itemsCannotBeModifiedFromOutside() {
        List<OrderItem> items = new ArrayList<>(List.of(lampItem()));
        Order order = Order.place(CUSTOMER_ID, items);

        items.add(shirtItem());

        assertThat(order.getItems()).hasSize(1);
        assertThatThrownBy(() -> order.getItems().add(shirtItem()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsOrderWithoutItems() {
        assertThatThrownBy(() -> Order.place(CUSTOMER_ID, List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("an order needs at least one item");
        assertThatThrownBy(() -> Order.place(CUSTOMER_ID, null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsTheSameProductTwice() {
        OrderItem again = OrderItem.of(1L, "Desk Lamp", new BigDecimal("45.00"), 1);

        assertThatThrownBy(() -> Order.place(CUSTOMER_ID, List.of(lampItem(), again)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("product 1 appears more than once");
    }

    @Test
    void rejectsMissingCustomer() {
        assertThatThrownBy(() -> Order.place(null, List.of(lampItem())))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("customerId is required");
    }

    @Test
    void requireDistinctProductsAcceptsDistinctIds() {
        assertThatCode(() -> Order.requireDistinctProducts(List.of(1L, 2L, 3L))).doesNotThrowAnyException();
    }
}
