package com.acme.orderservice.service;

import com.acme.common.core.Result;
import com.acme.common.logging.AuditLog;
import com.acme.domain.model.Money;
import com.acme.domain.model.Order;
import com.acme.domain.model.OrderLine;
import com.acme.persistence.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderAppService {

    private final AuditLog auditLog;
    private final OrderRepository repository;

    public OrderAppService(OrderRepository repository, AuditLog auditLog) {
        this.repository = repository;
        this.auditLog = auditLog;
    }

    public Result<Order> createOrder(String customerId, String sku, int quantity, String amount, String currency) {
        OrderLine line = new OrderLine(sku, quantity, Money.of(amount, currency));
        return Order.create(customerId, List.of(line)).map(order -> {
            Order saved = repository.save(order);
            auditLog.record("CREATE_ORDER", saved.id());
            return saved;
        });
    }
}
