package com.acme.orderservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.orderservice.service.OrderAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceIT {

    @Autowired
    private OrderAppService orderAppService;

    @Test
    void contextLoadsAndServiceInjected() {
        assertThat(orderAppService).isNotNull();
        var order = orderAppService.createOrder("IT-ORD-1", "CUST-IT", "USD");
        assertThat(order.getOrderId()).isEqualTo("IT-ORD-1");
    }
}
