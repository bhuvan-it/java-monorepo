package com.acme.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void testCreateValidProduct() {
        Money price = Money.of("19.99", "USD");
        Product product = Product.create("PROD-1", "Test Product", price).orElseThrow();
        assertEquals("PROD-1", product.sku());
        assertEquals("Test Product", product.name());
        assertEquals(price, product.price());
    }

    @Test
    void testCreateInvalidProduct() {
        assertFalse(Product.create("", "Test Product", Money.of("19.99", "USD")).isOk());
        assertFalse(Product.create("PROD-1", "", Money.of("19.99", "USD")).isOk());
        assertFalse(Product.create("PROD-1", "Test Product", Money.zero("USD")).isOk());
    }
}
