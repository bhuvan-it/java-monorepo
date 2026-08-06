package com.acme.domain.model;

import com.acme.common.core.Ids;
import com.acme.common.core.Result;
import java.util.List;
import java.util.Objects;

public record Order(String id, String customerId, List<OrderLine> lines) {

    public Order {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
    }

    public static Result<Order> create(String customerId, List<OrderLine> lines) {
        if (customerId == null || customerId.isBlank()) {
            return Result.err("customerId must not be blank");
        }
        if (lines == null || lines.isEmpty()) {
            return Result.err("an order needs at least one line");
        }
        long distinctCurrencies = lines.stream()
                .map(line -> line.unitPrice().currency())
                .distinct()
                .count();
        if (distinctCurrencies > 1) {
            return Result.err("all order lines must share one currency");
        }
        return Result.ok(new Order(Ids.newId("ord"), customerId, lines));
    }

    public Money total() {
        return lines.stream().map(OrderLine::lineTotal).reduce(Money::plus).orElseThrow();
    }

    public int itemCount() {
        return lines.stream().mapToInt(OrderLine::quantity).sum();
    }
}
