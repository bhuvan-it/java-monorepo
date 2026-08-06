package com.acme.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.domain.model.Money;
import com.acme.domain.model.Product;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryProductRepositoryTest {

    private InMemoryProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
    }

    @Test
    void shouldSaveAndFindProductBySku() {
        Product product = Product.create("PROD-1", "Test Product", Money.of("10.00", "USD")).orElseThrow();

        repository.save(product);

        Product found = repository.findBySku("PROD-1").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.name()).isEqualTo("Test Product");
    }

    @Test
    void shouldReturnAllSavedProducts() {
        Product p1 = Product.create("PROD-1", "P1", Money.of("10.00", "USD")).orElseThrow();
        Product p2 = Product.create("PROD-2", "P2", Money.of("20.00", "USD")).orElseThrow();

        repository.save(p1);
        repository.save(p2);

        List<Product> all = repository.findAll();
        assertThat(all).hasSize(2);
    }
}
