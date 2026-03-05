package com.hackathon.inventoryservice.api;

import java.util.Map;
import com.hackathon.inventoryservice.service.InventoryStockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryStockService inventoryStockService;

    public InventoryController(InventoryStockService inventoryStockService) {
        this.inventoryStockService = inventoryStockService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "inventory-service",
                "status", "UP"
        );
    }

    @GetMapping("/stocks")
    public Map<String, Integer> getAllStocks() {
        return inventoryStockService.getAllStocks();
    }

    @GetMapping("/stocks/{productId}")
    public Map<String, Object> getStock(@PathVariable String productId) {
        return Map.of(
                "productId", productId,
                "availableStock", inventoryStockService.getStock(productId)
        );
    }

    @PostMapping("/stocks/{productId}")
    public Map<String, Object> setStock(@PathVariable String productId, @RequestBody UpdateStockRequest request) {
        int updated = inventoryStockService.setStock(productId, request.getQuantity());
        return Map.of(
                "productId", productId,
                "availableStock", updated
        );
    }
}
