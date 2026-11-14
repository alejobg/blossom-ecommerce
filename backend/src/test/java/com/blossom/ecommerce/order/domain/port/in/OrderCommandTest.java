package com.blossom.ecommerce.order.domain.port.in;

import com.blossom.ecommerce.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCommandTest {

    @Test
    void lineRejectsQuantityBelowOne() {
        assertThatThrownBy(() -> new OrderLine(1L, 0))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("quantity must be at least 1");
    }

    @Test
    void lineRejectsMissingProduct() {
        assertThatThrownBy(() -> new OrderLine(null, 1))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("productId is required");
    }

    @Test
    void commandCopiesLinesDefensively() {
        List<OrderLine> lines = new ArrayList<>(List.of(new OrderLine(1L, 1)));
        PlaceOrderCommand command = new PlaceOrderCommand(7L, lines);

        lines.clear();

        assertThat(command.lines()).hasSize(1);
    }

    @Test
    void commandTreatsMissingLinesAsEmpty() {
        assertThat(new PlaceOrderCommand(7L, null).lines()).isEmpty();
    }
}
