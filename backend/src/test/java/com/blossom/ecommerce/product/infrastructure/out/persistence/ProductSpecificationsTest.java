package com.blossom.ecommerce.product.infrastructure.out.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSpecificationsTest {

    @Test
    void escapeLikeEscapesWildcardsAndEscapeCharacter() {
        assertThat(ProductSpecifications.escapeLike("50%_off\\")).isEqualTo("50\\%\\_off\\\\");
    }

    @Test
    void escapeLikeLeavesPlainTextUntouched() {
        assertThat(ProductSpecifications.escapeLike("lamp")).isEqualTo("lamp");
    }
}
