package com.acme.orderservice;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.acme.domain.model.Order;
import com.acme.orderservice.service.OrderAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceIT {

    @Autowired
    private OrderAppService orderAppService;

    @Test
    void testCreateOrderIntegration() {
        Order order = orderAppService.createOrder("IT-CUST-1", "IT-SKU-1", 1, "10.00", "USD");
        assertNotNull(order);
        assertNotNull(order.id());
    }
}
