package com.hackathon.inventoryservice.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InventoryStockService {
    private final Map<String, Integer> stockByProduct = new ConcurrentHashMap<>();

    public InventoryStockService() {
        stockByProduct.put("P-100", 50);
        stockByProduct.put("P-200", 30);
        stockByProduct.put("P-300", 10);
    }

    public synchronized ReservationResult reserve(String productId, int quantity) {
        int available = stockByProduct.getOrDefault(productId, 0);
        if (quantity <= 0) {
            return new ReservationResult("REJECTED", "INVALID_QUANTITY", available);
        }
        if (available < quantity) {
            return new ReservationResult("REJECTED", "OUT_OF_STOCK", available);
        }

        int remaining = available - quantity;
        stockByProduct.put(productId, remaining);
        return new ReservationResult("APPROVED", "STOCK_RESERVED", remaining);
    }

    public Map<String, Integer> getAllStocks() {
        return new HashMap<>(stockByProduct);
    }

    public int getStock(String productId) {
        return stockByProduct.getOrDefault(productId, 0);
    }

    public synchronized int setStock(String productId, int quantity) {
        stockByProduct.put(productId, Math.max(quantity, 0));
        return stockByProduct.get(productId);
    }

    public record ReservationResult(String status, String reason, int remainingStock) {}
}
