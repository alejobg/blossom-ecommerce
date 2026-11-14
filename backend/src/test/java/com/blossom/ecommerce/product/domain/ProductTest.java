package com.blossom.ecommerce.product.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static final BigDecimal PRICE = new BigDecimal("10.00");

    @Nested
    class Creation {

        @Test
        void createsActiveProductWithTrimmedFields() {
            Product product = Product.create("  Lamp  ", "  LED  ", PRICE, 5, ProductCategory.HOME);

            assertThat(product.getId()).isNull();
            assertThat(product.getName()).isEqualTo("Lamp");
            assertThat(product.getDescription()).isEqualTo("LED");
            assertThat(product.getPrice()).isEqualByComparingTo("10.00");
            assertThat(product.getStock()).isEqualTo(5);
            assertThat(product.getCategory()).isEqualTo(ProductCategory.HOME);
            assertThat(product.isActive()).isTrue();
        }

        @Test
        void blankDescriptionIsStoredAsNull() {
            Product product = Product.create("Lamp", "   ", PRICE, 0, ProductCategory.HOME);

            assertThat(product.getDescription()).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void rejectsMissingName(String name) {
            assertThatThrownBy(() -> Product.create(name, null, PRICE, 1, ProductCategory.HOME))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessage("name is required");
        }

        @Test
        void rejectsTooLongName() {
            String name = "a".repeat(Product.MAX_NAME_LENGTH + 1);

            assertThatThrownBy(() -> Product.create(name, null, PRICE, 1, ProductCategory.HOME))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void rejectsTooLongDescription() {
            String description = "a".repeat(Product.MAX_DESCRIPTION_LENGTH + 1);

            assertThatThrownBy(() -> Product.create("Lamp", description, PRICE, 1, ProductCategory.HOME))
                    .isInstanceOf(DomainValidationException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1.50"})
        void rejectsNonPositivePrice(String price) {
            assertThatThrownBy(() -> Product.create("Lamp", null, new BigDecimal(price), 1, ProductCategory.HOME))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessage("price must be greater than 0");
        }

        @Test
        void rejectsNullPrice() {
            assertThatThrownBy(() -> Product.create("Lamp", null, null, 1, ProductCategory.HOME))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void rejectsNegativeStock() {
            assertThatThrownBy(() -> Product.create("Lamp", null, PRICE, -1, ProductCategory.HOME))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void rejectsNullCategory() {
            assertThatThrownBy(() -> Product.create("Lamp", null, PRICE, 1, null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessage("category is required");
        }

        @Test
        void restoreRequiresId() {
            assertThatThrownBy(() -> Product.restore(null, "Lamp", null, PRICE, 1, ProductCategory.HOME, true))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Behaviour {

        private Product product() {
            return Product.restore(1L, "Lamp", null, PRICE, 3, ProductCategory.HOME, true);
        }

        @Test
        void updateReplacesDetailsAndKeepsIdentity() {
            Product product = product();

            product.update("Big Lamp", "Bright", new BigDecimal("20.00"), 7, ProductCategory.ELECTRONICS);

            assertThat(product.getId()).isEqualTo(1L);
            assertThat(product.getName()).isEqualTo("Big Lamp");
            assertThat(product.getPrice()).isEqualByComparingTo("20.00");
            assertThat(product.getStock()).isEqualTo(7);
            assertThat(product.getCategory()).isEqualTo(ProductCategory.ELECTRONICS);
        }

        @Test
        void updateValidatesLikeCreation() {
            Product product = product();

            assertThatThrownBy(() -> product.update("", null, PRICE, 1, ProductCategory.HOME))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void deactivateMarksProductAsInactive() {
            Product product = product();

            product.deactivate();

            assertThat(product.isActive()).isFalse();
        }

        @Test
        void hasStockChecksRequestedQuantity() {
            Product product = product();

            assertThat(product.hasStock(3)).isTrue();
            assertThat(product.hasStock(4)).isFalse();
            assertThat(product.hasStock(0)).isFalse();
        }

        @Test
        void decreaseStockSubtractsQuantity() {
            Product product = product();

            product.decreaseStock(2);

            assertThat(product.getStock()).isEqualTo(1);
        }

        @Test
        void decreaseStockFailsWhenNotEnoughUnits() {
            Product product = product();

            assertThatThrownBy(() -> product.decreaseStock(4))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Insufficient stock");
            assertThat(product.getStock()).isEqualTo(3);
        }

        @Test
        void decreaseStockRejectsNonPositiveQuantity() {
            Product product = product();

            assertThatThrownBy(() -> product.decreaseStock(0)).isInstanceOf(DomainValidationException.class);
        }
    }
}
