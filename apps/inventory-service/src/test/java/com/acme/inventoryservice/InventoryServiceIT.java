package com.acme.inventoryservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.inventoryservice.service.InventoryAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InventoryServiceIT {

    @Autowired
    private InventoryAppService inventoryAppService;

    @Test
    void contextLoadsAndServiceInjected() {
        assertThat(inventoryAppService).isNotNull();
        inventoryAppService.addStock("IT-PROD-1", 100);
        assertThat(inventoryAppService.getAvailableStock("IT-PROD-1")).isEqualTo(100);
    }
}
