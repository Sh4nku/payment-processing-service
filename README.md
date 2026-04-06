# Payment Processing Service

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![Kafka](https://img.shields.io/badge/Kafka-KRaft-black)
![Redis](https://img.shields.io/badge/Redis-7-red)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)

A production-grade payment processing microservice built for fintech use cases. Handles account management, ACID-compliant fund transfers, event-driven messaging, and real-time caching.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Payment Processing Service            │
│                       (Port 8080)                        │
│                                                          │
│  ┌──────────┐    ┌─────────────┐    ┌────────────────┐  │
│  │ Account  │    │ Transaction │    │  Audit Module  │  │
│  │  Module  │    │   Module    │    │                │  │
│  └────┬─────┘    └──────┬──────┘    └───────┬────────┘  │
│       │                 │                   │            │
│  ┌────▼─────────────────▼───────────────────▼────────┐  │
│  │              PostgreSQL (paymentdb)                │  │
│  └────────────────────────────────────────────────────┘  │
│                         │                                │
│  ┌──────────────────────▼─────────────────────────────┐  │
│  │            Kafka Producer (KRaft)                  │  │
│  │         Topic: payment-transactions                │  │
│  └────────────────────────────────────────────────────┘  │
│                         │                                │
│  ┌──────────────────────▼─────────────────────────────┐  │
│  │              Redis Cache                           │  │
│  │     accounts (5min TTL) | balances (1min TTL)      │  │
│  └────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          │
                          │ Kafka Event
                          ▼
            ┌─────────────────────────┐
            │  Fraud Detection Engine  │
            │       (Port 8081)        │
            └─────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 15 |
| Messaging | Apache Kafka (KRaft — no Zookeeper) |
| Cache | Redis 7 |
| Security | JWT (JJWT 0.12) |
| Resilience | Resilience4j (Circuit Breaker + Retry) |
| Testing | JUnit 5 + Mockito |
| DevOps | Docker Compose |

---

## Key Features

- **ACID-compliant fund transfers** with optimistic locking (versioned entities)
- **Idempotency keys** on all payment APIs — prevents duplicate transactions
- **Kafka event publishing** for every transaction (KRaft mode — no Zookeeper)
- **Immutable audit logs** via Kafka consumer
- **Redis caching** for accounts and balances with TTL
- **JWT-based stateless authentication**
- **Circuit breaker + retry** with Resilience4j
- **Global exception handling** with structured error responses

---

## Project Structure

```
src/main/java/com/payment/
├── account/
│   ├── controller/     → REST endpoints
│   ├── service/        → Business logic + Redis caching
│   ├── repository/     → JPA queries
│   ├── entity/         → Account, AccountStatus
│   └── dto/            → Request/Response models
├── transaction/
│   ├── controller/     → Transfer endpoints
│   ├── service/        → ACID transfer logic + Circuit Breaker
│   ├── repository/     → JPA queries
│   ├── entity/         → Transaction, TransactionType, TransactionStatus
│   └── dto/            → Transfer request/response
├── audit/              → Immutable audit log via Kafka consumer
├── kafka/              → Producer, Consumer, Event models
├── cache/              → Redis configuration
├── security/           → JWT filter, Security config, Auth controller
└── exception/          → Global error handling, ErrorCode enum
```

---

## Running Locally

### Prerequisites
- Java 21
- Maven
- Docker + Docker Compose

### Step 1 — Start Infrastructure
```bash
docker compose up -d
```
Starts: PostgreSQL (5432), Redis (6379), Kafka (9092)

### Step 2 — Run Application
```bash
mvn spring-boot:run
```
Application starts on **port 8080**

### Step 3 — Run Tests
```bash
mvn test
```

---

## API Reference

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/token` | Generate JWT token |

### Accounts
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/accounts` | Create account |
| GET | `/api/v1/accounts/{accountNumber}` | Get account details |
| GET | `/api/v1/accounts/{accountNumber}/balance` | Get balance |
| GET | `/api/v1/accounts` | List all accounts |
| PATCH | `/api/v1/accounts/{accountNumber}/status` | Update status |

### Transactions
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/transactions/transfer` | Transfer funds |
| GET | `/api/v1/transactions/{id}` | Get transaction |
| GET | `/api/v1/transactions/account/{accountNumber}` | Get by account |

---

## Sample API Calls

### 1. Generate Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "ashish"}'
```

### 2. Create Account
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "ownerName": "Ashish Singh",
    "email": "ashish@test.com",
    "initialBalance": 100000.00
  }'
```

### 3. Transfer Funds
```bash
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "PAY123456",
    "destinationAccountNumber": "PAY789012",
    "amount": 80000.00,
    "description": "Payment",
    "idempotencyKey": "unique-key-001"
  }'
```

---

## Design Decisions

**Why Kafka over REST for audit?**
Decouples payment processing from audit logging. If audit service is down, payments still process — events are replayed when audit comes back up.

**Why idempotency keys?**
Prevents duplicate charges in case of network retries — critical in financial systems.

**Why optimistic locking?**
Prevents race conditions in concurrent balance updates without expensive database locks.

**Why KRaft mode for Kafka?**
Removes Zookeeper dependency — simpler deployment, faster startup, production-preferred from Kafka 3.3+.

---

## Related Project
- [Fraud Detection Engine](https://github.com/Sh4nku/fraud-detection-engine) — consumes payment events and applies real-time fraud detection
