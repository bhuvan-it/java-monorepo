package com.acme.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.domain.model.Money;
import com.acme.domain.model.Order;
import com.acme.domain.model.OrderLine;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryOrderRepositoryTest {

    private InMemoryOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
    }

    @Test
    void shouldSaveAndFindOrderById() {
        OrderLine line = new OrderLine("SKU-1", 2, Money.of("10.00", "USD"));
        Order order = Order.create("CUST-1", List.of(line)).orElseThrow();

        repository.save(order);

        Order found = repository.findById(order.id()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.customerId()).isEqualTo("CUST-1");
        assertThat(found.lines()).hasSize(1);
    }

    @Test
    void shouldReturnAllSavedOrders() {
        OrderLine line1 = new OrderLine("SKU-1", 1, Money.of("5.00", "USD"));
        Order order1 = Order.create("CUST-1", List.of(line1)).orElseThrow();

        OrderLine line2 = new OrderLine("SKU-2", 1, Money.of("15.00", "USD"));
        Order order2 = Order.create("CUST-2", List.of(line2)).orElseThrow();

        repository.save(order1);
        repository.save(order2);

        List<Order> all = repository.findAll();
        assertThat(all).hasSize(2);
    }
}
