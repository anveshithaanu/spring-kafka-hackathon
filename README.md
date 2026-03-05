## Problem Statement

Build an event-driven microservices system using Spring Boot and Apache Kafka.

### Scenario

Users place orders. The system must:
- Validate inventory
- Perform fraud checks
- Confirm or reject the order

All communication must happen via Kafka events. No direct REST communication between services.

---

## Architecture Overview

Order Service → order-events → Inventory Service
Order Service → order-events → Fraud Service
Inventory Service → inventory-events → Order Service
Fraud Service → fraud-events → Order Service

---

## Topics

- order-events
- inventory-events
- fraud-events

Key = orderId  

---

## Functional Requirements

### Order Service
- POST /orders API
- Save order with status PENDING
- Publish OrderCreated event
- Listen for Inventory and Fraud responses
- Update final order status

### Inventory Service
- Consume OrderCreated
- Validate stock
- Publish InventoryApproved / InventoryRejected

### Fraud Service
- Consume OrderCreated
- If amount > 50000 → FraudRejected
- Otherwise → FraudApproved


## Expected Outcome

A fully event-driven system demonstrating:
- Kafka producers and consumers
- Consumer groups
- Event-driven architecture
- Decoupled microservices

---

## Implementation Included

This repository now contains:

- `order-service`
  - `POST /orders` endpoint
  - In-memory order store with `PENDING`, `APPROVED`, `REJECTED`
  - Publishes `OrderCreated` payload to `order-events`
  - Consumes `inventory-events` and `fraud-events` and finalizes order status
- `inventory-service`
  - Consumes `order-events`
  - Checks available stock by `productId`, reserves stock on approval
  - Publishes result to `inventory-events`
- `fraud-service`
  - Consumes `order-events`
  - Rejects when amount is `> 50000`, otherwise approves
  - Publishes result to `fraud-events`

All events are JSON and keyed by `orderId`.

---

## Run Locally

### 1) Start everything (Kafka + all services)

```bash
docker compose up --build -d
```

Ports:
- Order Service: `8081`
- Inventory Service: `8082`
- Fraud Service: `8083`

### 2) Create an order

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":"P-100","quantity":2,"amount":999.0}'
```

### 3) Check status

Use `orderId` from create response:

```bash
curl http://localhost:8081/orders/{orderId}
```

### 4) Stop everything

```bash
docker compose down
```

## Swagger UI

After containers are up, open:

- Order Service: `http://localhost:8081/`
- Inventory Service: `http://localhost:8082/`
- Fraud Service: `http://localhost:8083/`

## Inventory Stock APIs

Inventory service now uses in-memory stock per product (instead of fixed quantity threshold).

- View all stock:
  - `GET http://localhost:8082/inventory/stocks`
- View one product stock:
  - `GET http://localhost:8082/inventory/stocks/{productId}`
- Set stock for product:
  - `POST http://localhost:8082/inventory/stocks/{productId}`
  - Body: `{"quantity": 25}`

If UI is not accessible immediately, check startup logs:

```bash
docker compose logs -f order-service inventory-service fraud-service
```
