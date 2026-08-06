package com.acme.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.acme.domain.model.Money;
import com.acme.domain.model.Product;
import org.junit.jupiter.api.Test;

class InMemoryProductRepositoryTest {

    @Test
    void testSaveAndFind() {
        InMemoryProductRepository repo = new InMemoryProductRepository();
        Product product = Product.create("SKU-100", "Test Product", Money.of("19.99", "USD"))
                .orElseThrow();

        repo.save(product);
        assertTrue(repo.findById(product.id()).isPresent());
        assertTrue(repo.findBySku("SKU-100").isPresent());
        assertEquals(1, repo.findAll().size());
    }
}
