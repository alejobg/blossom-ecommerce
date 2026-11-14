package com.blossom.ecommerce.order.infrastructure.out.persistence;

import com.blossom.ecommerce.TestcontainersConfiguration;
import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderItem;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence adapter against a real PostgreSQL: mapping, ownership filter, ordering and query count.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, OrderPersistenceAdapter.class})
@ActiveProfiles("test")
class OrderPersistenceAdapterTest {

    @Autowired
    private OrderPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Long customerId;
    private Long otherCustomerId;
    private Long lampId;
    private Long shirtId;

    @BeforeEach
    void setUp() {
        customerId = insertCustomer("ana@mail.com");
        otherCustomerId = insertCustomer("bob@mail.com");
        lampId = insertProduct("Desk Lamp", "45.00");
        shirtId = insertProduct("Cotton T-Shirt", "19.99");
    }

    @Test
    void saveAssignsIdAndCreationTimeAndRoundTripsItems() {
        Order saved = adapter.save(order(customerId, 2));
        entityManager.clear();

        Order found = adapter.findByIdAndCustomerId(saved.getId(), customerId).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getCustomerId()).isEqualTo(customerId);
        assertThat(found.getTotal()).isEqualByComparingTo("109.99");
        assertThat(found.getItems()).extracting(OrderItem::getProductName).containsExactly("Desk Lamp", "Cotton T-Shirt");
        assertThat(found.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT total FROM orders WHERE id = ?", BigDecimal.class,
                saved.getId())).isEqualByComparingTo("109.99");
    }

    @Test
    void saveRejectsAlreadyPersistedOrders() {
        Order saved = adapter.save(order(customerId, 1));

        assertThatThrownBy(() -> adapter.save(saved)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findByIdOnlyReturnsTheOwnersOrder() {
        Order saved = adapter.save(order(customerId, 1));

        assertThat(adapter.findByIdAndCustomerId(saved.getId(), otherCustomerId)).isEmpty();
    }

    @Test
    void historyIsFilteredByCustomerNewestFirstAndPaginated() {
        Order first = adapter.save(order(customerId, 1));
        Order second = adapter.save(order(customerId, 2));
        Order third = adapter.save(order(customerId, 3));
        adapter.save(order(otherCustomerId, 1));
        entityManager.clear();

        PageResult<Order> firstPage = adapter.findByCustomerId(customerId, new PageQuery(0, 2));
        PageResult<Order> secondPage = adapter.findByCustomerId(customerId, new PageQuery(1, 2));

        assertThat(firstPage.content()).extracting(Order::getId).containsExactly(third.getId(), second.getId());
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).extracting(Order::getId).containsExactly(first.getId());
    }

    @Test
    void historyOfCustomerWithoutOrdersIsEmpty() {
        PageResult<Order> page = adapter.findByCustomerId(customerId, new PageQuery(0, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void historyLoadsItemsWithoutOneQueryPerOrder() {
        for (int i = 1; i <= 5; i++) {
            adapter.save(order(customerId, i));
        }
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        PageResult<Order> page = adapter.findByCustomerId(customerId, new PageQuery(0, 20));

        assertThat(page.content()).hasSize(5).allSatisfy(order -> assertThat(order.getItems()).hasSize(2));
        // ids page + orders with items (the count query is skipped when the first page is not full)
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    private Order order(Long owner, int lampQuantity) {
        return Order.place(owner, List.of(
                OrderItem.of(lampId, "Desk Lamp", new BigDecimal("45.00"), lampQuantity),
                OrderItem.of(shirtId, "Cotton T-Shirt", new BigDecimal("19.99"), 1)));
    }

    private Long insertCustomer(String email) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (role, name, email, password_hash, created_at, updated_at)
                VALUES ('CUSTOMER', 'Customer', ?, '$2a$10$hash', NOW(), NOW()) RETURNING id
                """, Long.class, email);
    }

    private Long insertProduct(String name, String price) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO products (name, price, stock, category, active, created_at, updated_at)
                VALUES (?, ?, 10, 'HOME', TRUE, NOW(), NOW()) RETURNING id
                """, Long.class, name, new BigDecimal(price));
    }
}
