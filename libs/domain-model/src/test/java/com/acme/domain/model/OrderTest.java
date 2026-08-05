package com.acme.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.common.core.Money;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void shouldCalculateTotalForOrderLines() {
        Order order = new Order("ORD-1", "CUST-100", "USD");
        order.addLine(new OrderLine("ITEM-A", 2, Money.of(10.0, "USD")));
        order.addLine(new OrderLine("ITEM-B", 1, Money.of(25.0, "USD")));

        assertThat(order.total()).isEqualTo(Money.of(45.0, "USD"));
    }

    @Test
    void shouldRejectMismatchingCurrencyLine() {
        Order order = new Order("ORD-1", "CUST-100", "USD");
        OrderLine lineInEur = new OrderLine("ITEM-A", 1, Money.of(10.0, "EUR"));

        assertThatThrownBy(() -> order.addLine(lineInEur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match order currency");
    }
}
