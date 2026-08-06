package com.acme.inventoryservice.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.acme.inventoryservice.service.InventoryAppService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class InventoryControllerTest {

    @Test
    void testInventoryControllerEndpoints() {
        InventoryAppService service = new InventoryAppService();
        InventoryController controller = new InventoryController(service);

        ResponseEntity<Map<String, String>> status = controller.status();
        assertEquals("UP", status.getBody().get("status"));

        controller.addStock("PROD-1", 10);
        assertEquals(10, controller.getStock("PROD-1").getBody());
    }
}
