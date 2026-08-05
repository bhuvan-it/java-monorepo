package com.acme.inventoryservice.service;

import com.acme.common.logging.StructuredLogger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InventoryAppService {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();
    private final StructuredLogger logger = new StructuredLogger("INVENTORY-SERVICE");

    public void addStock(String productId, int quantity) {
        stock.merge(productId, quantity, Integer::sum);
        logger.formatLog("ADD_STOCK", productId, null);
    }

    public boolean reserveStock(String productId, int quantity) {
        Integer current = stock.get(productId);
        if (current != null && current >= quantity) {
            stock.put(productId, current - quantity);
            logger.formatLog("RESERVE_STOCK", productId, null);
            return true;
        }
        return false;
    }

    public int getAvailableStock(String productId) {
        return stock.getOrDefault(productId, 0);
    }
}
