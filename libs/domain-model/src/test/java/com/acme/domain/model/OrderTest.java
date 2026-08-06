package com.acme.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void testCreateValidOrder() {
        OrderLine line = new OrderLine("SKU-1", 2, Money.of("10.00", "USD"));
        Order order = Order.create("CUST-1", List.of(line)).orElseThrow();
        assertThat(order.customerId()).isEqualTo("CUST-1");
        assertThat(order.total()).isEqualTo(Money.of("20.00", "USD"));
    }

    @Test
    void testCreateInvalidOrder() {
        assertThat(Order.create("", List.of()).isOk()).isFalse();
    }
}
