package com.blossom.ecommerce.shared;

import com.blossom.ecommerce.shared.domain.DomainValidationException;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageModelTest {

    @ParameterizedTest
    @CsvSource({"-1, 10", "0, 0", "0, 101"})
    void pageQueryRejectsInvalidValues(int page, int size) {
        assertThatThrownBy(() -> new PageQuery(page, size)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void pageQueryAcceptsBoundaryValues() {
        assertThat(new PageQuery(0, 1).size()).isEqualTo(1);
        assertThat(new PageQuery(3, PageQuery.MAX_SIZE).page()).isEqualTo(3);
    }

    @Test
    void pageResultMapsContentAndKeepsMetadata() {
        PageResult<Integer> result = new PageResult<>(List.of(1, 2), 1, 2, 5, 3);

        PageResult<String> mapped = result.map(n -> "#" + n);

        assertThat(mapped.content()).containsExactly("#1", "#2");
        assertThat(mapped.page()).isEqualTo(1);
        assertThat(mapped.size()).isEqualTo(2);
        assertThat(mapped.totalElements()).isEqualTo(5);
        assertThat(mapped.totalPages()).isEqualTo(3);
    }

    @Test
    void pageResultIsNotAffectedByChangesToTheSourceList() {
        List<Integer> source = new ArrayList<>(List.of(1));
        PageResult<Integer> result = new PageResult<>(source, 0, 1, 1, 1);

        source.add(2);

        assertThat(result.content()).containsExactly(1);
    }
}
