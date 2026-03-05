package com.hackathon.fraudservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class FraudEventHandler {
    private static final double FRAUD_THRESHOLD = 50_000.0;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FraudEventHandler(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "fraud-service-group")
    public void onOrderCreated(String payload) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(payload);
        String orderId = node.path("orderId").asText();
        double amount = node.path("amount").asDouble();

        String status = amount > FRAUD_THRESHOLD ? "REJECTED" : "APPROVED";
        String reason = "APPROVED".equals(status) ? "NO_FRAUD_DETECTED" : "FRAUD_RULE_THRESHOLD_EXCEEDED";

        publishFraudEvent(orderId, status, reason);
    }

    private void publishFraudEvent(String orderId, String status, String reason) {
        try {
            String responsePayload = objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "status", status,
                    "reason", reason
            ));
            kafkaTemplate.send("fraud-events", orderId, responsePayload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to publish fraud event", e);
        }
    }
}
