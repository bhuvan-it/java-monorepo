package com.acme.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.common.logging.AuditLog;
import com.acme.domain.model.Money;
import com.acme.domain.model.Order;
import com.acme.persistence.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

class OrderAppServiceTest {

    @Test
    void testCreateOrder() {
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        AuditLog auditLog = new AuditLog("TEST");
        OrderAppService service = new OrderAppService(repo, auditLog);

        Order order = service.createOrder("CUST-1", "SKU-1", 2, "10.00", "USD").orElseThrow();
        assertThat(order.id()).isNotNull();
        assertThat(order.total()).isEqualTo(Money.of("20.00", "USD"));
    }
}
