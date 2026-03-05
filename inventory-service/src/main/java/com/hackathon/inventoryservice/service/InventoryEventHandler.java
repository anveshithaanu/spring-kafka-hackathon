package com.hackathon.inventoryservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventHandler {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final InventoryStockService inventoryStockService;

    public InventoryEventHandler(KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 InventoryStockService inventoryStockService) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.inventoryStockService = inventoryStockService;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    public void onOrderCreated(String payload) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(payload);
        String orderId = node.path("orderId").asText();
        String productId = node.path("productId").asText();
        int quantity = node.path("quantity").asInt();

        InventoryStockService.ReservationResult result = inventoryStockService.reserve(productId, quantity);

        publishInventoryEvent(orderId, productId, result.status(), result.reason(), result.remainingStock());
    }

    private void publishInventoryEvent(String orderId, String productId, String status, String reason, int remainingStock) {
        try {
            String responsePayload = objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "productId", productId,
                    "status", status,
                    "reason", reason,
                    "remainingStock", remainingStock
            ));
            kafkaTemplate.send("inventory-events", orderId, responsePayload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to publish inventory event", e);
        }
    }
}
