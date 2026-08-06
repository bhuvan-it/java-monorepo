package com.acme.persistence;

import com.acme.domain.model.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String id);
    List<Order> findAll();
}
