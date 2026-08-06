package com.acme.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.acme.common.core.Result;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void testOrderCreationAndTotal() {
        OrderLine line1 = new OrderLine("SKU-1", 2, Money.of("10.00", "USD"));
        OrderLine line2 = new OrderLine("SKU-2", 1, Money.of("25.00", "USD"));

        Result<Order> result = Order.create("CUST-100", List.of(line1, line2));
        assertTrue(result.isOk());

        Order order = result.orElseThrow();
        assertEquals(3, order.itemCount());
        assertEquals(Money.of("45.00", "USD"), order.total());
    }

    @Test
    void testCurrencyMismatchInLines() {
        OrderLine line1 = new OrderLine("SKU-1", 1, Money.of("10.00", "USD"));
        OrderLine line2 = new OrderLine("SKU-2", 1, Money.of("10.00", "EUR"));

        Result<Order> result = Order.create("CUST-100", List.of(line1, line2));
        assertTrue(result instanceof Result.Err);
    }
}
