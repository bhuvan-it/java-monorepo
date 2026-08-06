package com.acme.orderservice.controller;

import com.acme.common.core.Result;
import com.acme.domain.model.Order;
import com.acme.orderservice.service.OrderAppService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderAppService service;

    public OrderController(OrderAppService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestParam String customerId,
            @RequestParam String sku,
            @RequestParam int quantity,
            @RequestParam String amount,
            @RequestParam String currency) {
        Result<Order> result = service.createOrder(customerId, sku, quantity, amount, currency);
        return switch (result) {
            case Result.Ok<Order> ok -> ResponseEntity.ok(ok.value());
            case Result.Err<Order> err -> ResponseEntity.badRequest().body(Map.of("error", err.message()));
        };
    }
}
