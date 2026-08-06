package com.acme.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.acme.domain.model.Money;
import com.acme.domain.model.Order;
import com.acme.domain.model.OrderLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryOrderRepositoryTest {

    @Test
    void testSaveAndFind() {
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        OrderLine line = new OrderLine("SKU-1", 1, Money.of("10.00", "USD"));
        Order order = Order.create("CUST-1", List.of(line)).orElseThrow();

        repo.save(order);
        assertEquals(1, repo.count());
        assertTrue(repo.findById(order.id()).isPresent());
        assertEquals("CUST-1", repo.findById(order.id()).get().customerId());
    }
}
