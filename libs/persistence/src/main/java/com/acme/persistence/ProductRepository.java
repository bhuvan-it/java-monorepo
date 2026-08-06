package com.acme.persistence;

import com.acme.domain.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(String id);

    Optional<Product> findBySku(String sku);

    List<Product> findAll();
}
