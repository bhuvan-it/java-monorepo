package com.acme.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void testCreateValidProduct() {
        Money price = Money.of("19.99", "USD");
        Product product = Product.create("PROD-1", "Test Product", price).orElseThrow();
        assertThat(product.sku()).isEqualTo("PROD-1");
        assertThat(product.name()).isEqualTo("Test Product");
        assertThat(product.price()).isEqualTo(price);
    }

    @Test
    void testCreateInvalidProduct() {
        assertThat(Product.create("", "Test Product", Money.of("19.99", "USD")).isOk()).isFalse();
        assertThat(Product.create("PROD-1", "", Money.of("19.99", "USD")).isOk()).isFalse();
        assertThat(Product.create("PROD-1", "Test Product", Money.zero("USD")).isOk()).isFalse();
    }
}
