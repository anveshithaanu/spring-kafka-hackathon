package com.hackathon.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.orderservice.api.CreateOrderRequest;
import com.hackathon.orderservice.model.OrderRecord;
import com.hackathon.orderservice.model.OrderStatus;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderWorkflowService {
    private static final String APPROVED = "APPROVED";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, OrderRecord> orders = new ConcurrentHashMap<>();

    public OrderWorkflowService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public OrderRecord createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();

        OrderRecord order = new OrderRecord();
        order.setOrderId(orderId);
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setAmount(request.getAmount());
        order.setStatus(OrderStatus.PENDING);
        orders.put(orderId, order);

        publishOrderCreated(order);
        return order;
    }

    public Optional<OrderRecord> getOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    @KafkaListener(topics = "inventory-events", groupId = "order-service-group")
    public void onInventoryEvent(String payload) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(payload);
        String orderId = node.path("orderId").asText();
        String status = node.path("status").asText();

        OrderRecord order = orders.get(orderId);
        if (order == null) {
            return;
        }

        synchronized (order) {
            order.setInventoryStatus(status);
            finalizeOrderIfComplete(order);
        }
    }

    @KafkaListener(topics = "fraud-events", groupId = "order-service-group")
    public void onFraudEvent(String payload) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(payload);
        String orderId = node.path("orderId").asText();
        String status = node.path("status").asText();

        OrderRecord order = orders.get(orderId);
        if (order == null) {
            return;
        }

        synchronized (order) {
            order.setFraudStatus(status);
            finalizeOrderIfComplete(order);
        }
    }

    private void finalizeOrderIfComplete(OrderRecord order) {
        if (order.getInventoryStatus() == null || order.getFraudStatus() == null) {
            return;
        }

        boolean approved = APPROVED.equalsIgnoreCase(order.getInventoryStatus())
                && APPROVED.equalsIgnoreCase(order.getFraudStatus());
        order.setStatus(approved ? OrderStatus.APPROVED : OrderStatus.REJECTED);
    }

    private void publishOrderCreated(OrderRecord order) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "orderId", order.getOrderId(),
                    "productId", order.getProductId(),
                    "quantity", order.getQuantity(),
                    "amount", order.getAmount()
            ));
            kafkaTemplate.send("order-events", order.getOrderId(), payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to publish order-created event", e);
        }
    }
}
