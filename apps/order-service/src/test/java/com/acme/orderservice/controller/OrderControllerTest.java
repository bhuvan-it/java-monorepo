package com.acme.orderservice.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.acme.domain.model.Order;
import com.acme.orderservice.service.OrderAppService;
import com.acme.persistence.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

class OrderControllerTest {

    @Test
    void testCreateOrderEndpoint() {
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        OrderAppService service = new OrderAppService(repo);
        OrderController controller = new OrderController(service);

        Order order = controller.createOrder("CUST-100", "SKU-1", 1, "15.00", "USD");
        assertNotNull(order.id());
    }
}
