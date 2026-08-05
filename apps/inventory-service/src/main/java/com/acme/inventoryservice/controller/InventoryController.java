package com.acme.inventoryservice.controller;

import com.acme.inventoryservice.service.InventoryAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryAppService service;

    public InventoryController(InventoryAppService service) {
        this.service = service;
    }

    @PostMapping("/stock")
    public ResponseEntity<Void> addStock(@RequestParam String productId, @RequestParam int quantity) {
        service.addStock(productId, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stock/{productId}")
    public ResponseEntity<Integer> getStock(@PathVariable String productId) {
        return ResponseEntity.ok(service.getAvailableStock(productId));
    }
}
