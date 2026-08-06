package com.acme.orderservice.controller;

import com.acme.domain.model.Order;
import com.acme.orderservice.service.OrderAppService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderAppService service;

    public OrderController(OrderAppService service) {
        this.service = service;
    }

    @PostMapping
    public Order createOrder(
            @RequestParam String customerId,
            @RequestParam String sku,
            @RequestParam int quantity,
            @RequestParam String amount,
            @RequestParam String currency) {
        return service.createOrder(customerId, sku, quantity, amount, currency);
    }
}
