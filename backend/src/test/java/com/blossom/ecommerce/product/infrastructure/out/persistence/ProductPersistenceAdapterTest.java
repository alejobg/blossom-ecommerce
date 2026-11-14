package com.blossom.ecommerce.product.infrastructure.out.persistence;

import com.blossom.ecommerce.TestcontainersConfiguration;
import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductCategory;
import com.blossom.ecommerce.product.domain.ProductFilter;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence adapter against a real PostgreSQL: mapping, soft delete and every search criterion.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ProductPersistenceAdapter.class})
@ActiveProfiles("test")
class ProductPersistenceAdapterTest {

    private static final PageQuery FIRST_PAGE = new PageQuery(0, 20);

    @Autowired
    private ProductPersistenceAdapter adapter;

    private Product lamp;
    private Product headphones;
    private Product tShirt;

    @BeforeEach
    void setUp() {
        lamp = adapter.save(Product.create("Desk Lamp", "LED", new BigDecimal("45.00"), 10, ProductCategory.HOME));
        headphones = adapter.save(Product.create("Wireless Headphones", null, new BigDecimal("189.99"), 5,
                ProductCategory.ELECTRONICS));
        tShirt = adapter.save(Product.create("Cotton T-Shirt", null, new BigDecimal("19.99"), 100,
                ProductCategory.CLOTHING));
    }

    @Test
    void saveAssignsIdAndRoundTripsAllFields() {
        Product found = adapter.findActiveById(lamp.getId()).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Desk Lamp");
        assertThat(found.getDescription()).isEqualTo("LED");
        assertThat(found.getPrice()).isEqualByComparingTo("45.00");
        assertThat(found.getStock()).isEqualTo(10);
        assertThat(found.getCategory()).isEqualTo(ProductCategory.HOME);
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void saveUpdatesExistingProduct() {
        lamp.update("Floor Lamp", null, new BigDecimal("80.00"), 2, ProductCategory.HOME);

        adapter.save(lamp);

        Product found = adapter.findActiveById(lamp.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Floor Lamp");
        assertThat(found.getPrice()).isEqualByComparingTo("80.00");
    }

    @Test
    void saveFailsWhenUpdatingUnknownId() {
        Product ghost = Product.restore(9999L, "Ghost", null, BigDecimal.ONE, 1, ProductCategory.HOME, true);

        assertThatThrownBy(() -> adapter.save(ghost)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivatedProductsAreHiddenFromLookupsAndSearch() {
        lamp.deactivate();
        adapter.save(lamp);

        assertThat(adapter.findActiveById(lamp.getId())).isEmpty();
        assertThat(adapter.searchActive(ProductFilter.empty(), FIRST_PAGE).content())
                .extracting(Product::getId)
                .containsExactlyInAnyOrder(headphones.getId(), tShirt.getId());
    }

    @Test
    void searchWithoutCriteriaReturnsAllSortedByName() {
        PageResult<Product> result = adapter.searchActive(ProductFilter.empty(), FIRST_PAGE);

        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.content()).extracting(Product::getName)
                .containsExactly("Cotton T-Shirt", "Desk Lamp", "Wireless Headphones");
    }

    @Test
    void searchByNameIsCaseInsensitiveAndPartial() {
        PageResult<Product> result = adapter.searchActive(new ProductFilter("LAMP", null, null, null), FIRST_PAGE);

        assertThat(result.content()).extracting(Product::getId).containsExactly(lamp.getId());
    }

    @Test
    void searchByNameTreatsWildcardsLiterally() {
        PageResult<Product> result = adapter.searchActive(new ProductFilter("%", null, null, null), FIRST_PAGE);

        assertThat(result.content()).isEmpty();
    }

    @Test
    void searchByCategory() {
        PageResult<Product> result = adapter.searchActive(
                new ProductFilter(null, ProductCategory.ELECTRONICS, null, null), FIRST_PAGE);

        assertThat(result.content()).extracting(Product::getId).containsExactly(headphones.getId());
    }

    @Test
    void searchByPriceRangeIsInclusive() {
        PageResult<Product> result = adapter.searchActive(
                new ProductFilter(null, null, new BigDecimal("19.99"), new BigDecimal("45.00")), FIRST_PAGE);

        assertThat(result.content()).extracting(Product::getId)
                .containsExactlyInAnyOrder(lamp.getId(), tShirt.getId());
    }

    @Test
    void searchCombinesCriteria() {
        PageResult<Product> result = adapter.searchActive(
                new ProductFilter("o", ProductCategory.CLOTHING, null, new BigDecimal("50")), FIRST_PAGE);

        assertThat(result.content()).extracting(Product::getId).containsExactly(tShirt.getId());
    }

    @Test
    void findForUpdateReturnsActiveProductsSortedByIdAndSkipsInactiveOnes() {
        tShirt.deactivate();
        adapter.save(tShirt);

        List<Product> locked = adapter.findActiveByIdsForUpdate(
                List.of(headphones.getId(), tShirt.getId(), lamp.getId(), 9999L));

        assertThat(locked).extracting(Product::getId).containsExactly(lamp.getId(), headphones.getId());
    }

    @Test
    void findForUpdateWithNoIdsReturnsEmpty() {
        assertThat(adapter.findActiveByIdsForUpdate(List.of())).isEmpty();
    }

    @Test
    void searchPaginates() {
        PageResult<Product> secondPage = adapter.searchActive(ProductFilter.empty(), new PageQuery(1, 2));

        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.size()).isEqualTo(2);
        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).extracting(Product::getName).containsExactly("Wireless Headphones");
    }
}
