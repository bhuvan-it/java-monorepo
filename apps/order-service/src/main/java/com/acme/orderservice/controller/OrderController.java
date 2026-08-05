package com.acme.orderservice.controller;

import com.acme.domain.model.Order;
import com.acme.orderservice.service.OrderAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderAppService service;

    public OrderController(OrderAppService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestParam String orderId,
            @RequestParam String customerId,
            @RequestParam(defaultValue = "USD") String currency) {
        Order order = service.createOrder(orderId, customerId, currency);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        return service.getOrder(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
