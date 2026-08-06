package com.acme.inventoryservice.service;

import com.acme.common.logging.StructuredLogger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InventoryAppService {

    private final StructuredLogger logger = new StructuredLogger("INVENTORY-SERVICE");
    private final Map<String, Integer> stockMap = new ConcurrentHashMap<>();

    public void addStock(String sku, int quantity) {
        stockMap.merge(sku, quantity, Integer::sum);
        logger.formatLog("ADD_STOCK", sku, quantity);
    }

    public boolean reserveStock(String sku, int quantity) {
        Integer current = stockMap.getOrDefault(sku, 0);
        if (current >= quantity) {
            stockMap.put(sku, current - quantity);
            logger.formatLog("RESERVE_STOCK", sku, quantity);
            return true;
        }
        return false;
    }

    public int getStock(String sku) {
        return stockMap.getOrDefault(sku, 0);
    }

    public int getAvailableStock(String sku) {
        return getStock(sku);
    }
}
