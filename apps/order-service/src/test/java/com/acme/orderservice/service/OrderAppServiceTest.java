package com.acme.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.common.core.Money;
import com.acme.domain.model.Order;
import com.acme.persistence.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderAppServiceTest {

    private InMemoryOrderRepository repository;
    private OrderAppService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        service = new OrderAppService(repository);
    }

    @Test
    void shouldCreateOrderAndAddLine() {
        Order order = service.createOrder("ORD-1", "CUST-1", "USD");
        assertThat(order.getOrderId()).isEqualTo("ORD-1");

        Order updated = service.addLineItem("ORD-1", "PROD-A", 3, 10.0);
        assertThat(updated.total()).isEqualTo(Money.of(30.0, "USD"));
    }
}
