package com.acme.orderservice.service;

import static org.junit.jupiter.api.Assertions.*;

import com.acme.domain.model.Money;
import com.acme.domain.model.Order;
import com.acme.persistence.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

class OrderAppServiceTest {

    @Test
    void testCreateOrder() {
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        OrderAppService service = new OrderAppService(repo);

        Order order = service.createOrder("CUST-1", "SKU-1", 2, "10.00", "USD");
        assertNotNull(order.id());
        assertEquals(Money.of("20.00", "USD"), order.total());
    }
}
