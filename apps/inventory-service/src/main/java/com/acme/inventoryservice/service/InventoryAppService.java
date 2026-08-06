package com.acme.inventoryservice.service;

import com.acme.common.logging.AuditLog;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InventoryAppService {

    private final AuditLog auditLog;
    private final Map<String, Integer> stockMap = new ConcurrentHashMap<>();

    public InventoryAppService(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    public void addStock(String sku, int quantity) {
        stockMap.merge(sku, quantity, Integer::sum);
        auditLog.record("ADD_STOCK", sku);
    }

    public boolean reserveStock(String sku, int quantity) {
        Integer current = stockMap.getOrDefault(sku, 0);
        if (current >= quantity) {
            stockMap.put(sku, current - quantity);
            auditLog.record("RESERVE_STOCK", sku);
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
