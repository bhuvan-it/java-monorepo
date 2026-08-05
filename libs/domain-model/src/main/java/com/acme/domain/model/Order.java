package com.acme.domain.model;

import com.acme.common.core.Money;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order {

    private final String orderId;
    private final String customerId;
    private final String currency;
    private final List<OrderLine> lines = new ArrayList<>();

    public Order(String orderId, String customerId, String currency) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.currency =
                Objects.requireNonNull(currency, "currency must not be null").toUpperCase();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCurrency() {
        return currency;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public void addLine(OrderLine line) {
        Objects.requireNonNull(line, "line must not be null");
        if (!line.getUnitPrice().getCurrency().equals(this.currency)) {
            throw new IllegalArgumentException("Line currency "
                    + line.getUnitPrice().getCurrency() + " does not match order currency " + this.currency);
        }
        lines.add(line);
    }

    public Money total() {
        Money sum = Money.zero(currency);
        for (OrderLine line : lines) {
            sum = sum.add(line.totalPrice());
        }
        return sum;
    }
}
