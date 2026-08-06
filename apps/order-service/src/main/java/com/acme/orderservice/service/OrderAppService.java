package com.acme.orderservice.service;

import com.acme.common.logging.StructuredLogger;
import com.acme.domain.model.Money;
import com.acme.domain.model.Order;
import com.acme.domain.model.OrderLine;
import com.acme.persistence.InMemoryOrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderAppService {

    private final StructuredLogger logger = new StructuredLogger("ORDER-SERVICE");
    private final InMemoryOrderRepository repository;

    public OrderAppService(InMemoryOrderRepository repository) {
        this.repository = repository;
    }

    public Order createOrder(String customerId, String sku, int quantity, String amount, String currency) {
        OrderLine line = new OrderLine(sku, quantity, Money.of(amount, currency));
        Order order = Order.create(customerId, List.of(line)).orElseThrow();
        Order saved = repository.save(order);
        logger.formatLog("CREATE_ORDER", saved.id(), saved.total());
        return saved;
    }
}
