package com.acme.inventoryservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryAppServiceTest {

    private InventoryAppService service;

    @BeforeEach
    void setUp() {
        service = new InventoryAppService();
    }

    @Test
    void shouldAddAndReserveStock() {
        service.addStock("PROD-1", 10);
        assertThat(service.getAvailableStock("PROD-1")).isEqualTo(10);

        boolean reserved = service.reserveStock("PROD-1", 4);
        assertThat(reserved).isTrue();
        assertThat(service.getAvailableStock("PROD-1")).isEqualTo(6);
    }

    @Test
    void shouldFailToReserveInsufficientStock() {
        service.addStock("PROD-1", 2);
        boolean reserved = service.reserveStock("PROD-1", 5);

        assertThat(reserved).isFalse();
        assertThat(service.getAvailableStock("PROD-1")).isEqualTo(2);
    }
}
