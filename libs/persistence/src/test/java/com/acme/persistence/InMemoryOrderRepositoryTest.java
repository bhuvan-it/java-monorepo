package com.acme.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.domain.model.Order;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryOrderRepositoryTest {

    @Test
    void shouldSaveAndFindOrder() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        Order order = new Order("ORD-99", "CUST-1", "USD");

        repository.save(order);
        Optional<Order> retrieved = repository.findById("ORD-99");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCustomerId()).isEqualTo("CUST-1");
    }

    @Test
    void shouldReturnEmptyForNonExistentOrder() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        assertThat(repository.findById("NON_EXISTENT")).isEmpty();
    }
}
