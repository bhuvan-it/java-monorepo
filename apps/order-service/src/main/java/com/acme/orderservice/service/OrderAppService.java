package com.acme.orderservice.service;

import com.acme.common.core.Money;
import com.acme.common.logging.StructuredLogger;
import com.acme.domain.model.Order;
import com.acme.domain.model.OrderLine;
import com.acme.persistence.InMemoryOrderRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OrderAppService {

    private final InMemoryOrderRepository repository;
    private final StructuredLogger logger = new StructuredLogger("ORDER-SERVICE");

    public OrderAppService(InMemoryOrderRepository repository) {
        this.repository = repository;
    }

    public Order createOrder(String orderId, String customerId, String currency) {
        Order order = new Order(orderId, customerId, currency);
        repository.save(order);
        logger.formatLog("CREATE_ORDER", orderId, Money.zero(currency));
        return order;
    }

    public Order addLineItem(String orderId, String productId, int quantity, double price) {
        Order order = repository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        Money unitPrice = Money.of(price, order.getCurrency());
        order.addLine(new OrderLine(productId, quantity, unitPrice));
        repository.save(order);
        logger.formatLog("ADD_LINE", orderId, unitPrice);
        return order;
    }

    public Optional<Order> getOrder(String orderId) {
        return repository.findById(orderId);
    }
}
