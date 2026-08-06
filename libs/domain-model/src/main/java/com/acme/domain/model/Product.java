package com.acme.domain.model;

import com.acme.common.core.Ids;
import com.acme.common.core.Result;
import java.util.Objects;

public record Product(String id, String sku, String name, Money price) {

    public Product {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(price, "price");
    }

    public static Result<Product> create(String sku, String name, Money price) {
        if (sku == null || sku.isBlank()) {
            return Result.err("sku must not be blank");
        }
        if (name == null || name.isBlank()) {
            return Result.err("name must not be blank");
        }
        if (price == null || !price.isPositive()) {
            return Result.err("price must be positive");
        }
        return Result.ok(new Product(Ids.newId("prd"), sku, name, price));
    }
}
