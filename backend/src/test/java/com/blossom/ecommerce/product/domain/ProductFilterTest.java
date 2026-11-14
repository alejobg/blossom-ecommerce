package com.blossom.ecommerce.product.domain;

import com.blossom.ecommerce.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductFilterTest {

    @Test
    void emptyFilterHasNoCriteria() {
        ProductFilter filter = ProductFilter.empty();

        assertThat(filter.name()).isNull();
        assertThat(filter.category()).isNull();
        assertThat(filter.minPrice()).isNull();
        assertThat(filter.maxPrice()).isNull();
    }

    @Test
    void blankNameIsIgnoredAndNameIsTrimmed() {
        assertThat(new ProductFilter("  ", null, null, null).name()).isNull();
        assertThat(new ProductFilter(" lamp ", null, null, null).name()).isEqualTo("lamp");
    }

    @Test
    void acceptsEqualMinAndMaxPrice() {
        BigDecimal price = new BigDecimal("10");

        assertThat(new ProductFilter(null, null, price, price).minPrice()).isEqualByComparingTo(price);
    }

    @Test
    void rejectsMinPriceGreaterThanMaxPrice() {
        assertThatThrownBy(() -> new ProductFilter(null, null, new BigDecimal("20"), new BigDecimal("10")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("minPrice must be less than or equal to maxPrice");
    }

    @Test
    void rejectsNegativePrices() {
        assertThatThrownBy(() -> new ProductFilter(null, null, new BigDecimal("-1"), null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new ProductFilter(null, null, null, new BigDecimal("-1")))
                .isInstanceOf(DomainValidationException.class);
    }
}
