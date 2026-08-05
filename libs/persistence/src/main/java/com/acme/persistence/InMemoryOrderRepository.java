package com.acme.persistence;

import com.acme.domain.model.Order;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOrderRepository {

    private final Map<String, Order> storage = new ConcurrentHashMap<>();

    public Order save(Order order) {
        storage.put(order.getOrderId(), order);
        return order;
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(storage.get(orderId));
    }

    public void deleteById(String orderId) {
        storage.remove(orderId);
    }

    public int count() {
        return storage.size();
    }
}
