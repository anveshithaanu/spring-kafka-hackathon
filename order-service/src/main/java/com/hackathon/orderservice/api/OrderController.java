package com.hackathon.orderservice.api;

import com.hackathon.orderservice.model.OrderRecord;
import com.hackathon.orderservice.service.OrderWorkflowService;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderWorkflowService orderWorkflowService;

    public OrderController(OrderWorkflowService orderWorkflowService) {
        this.orderWorkflowService = orderWorkflowService;
    }

    @PostMapping
    public ResponseEntity<OrderRecord> createOrder(@RequestBody CreateOrderRequest request) {
        if (request.getProductId() == null || request.getProductId().isBlank()
                || request.getQuantity() <= 0
                || request.getAmount() < 0) {
            return ResponseEntity.badRequest().build();
        }

        OrderRecord createdOrder = orderWorkflowService.createOrder(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(createdOrder);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderRecord> getOrder(@PathVariable String orderId) {
        Optional<OrderRecord> order = orderWorkflowService.getOrder(orderId);
        return order.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
