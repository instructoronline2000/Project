# Test Manual — Seed Data & Manual/Integration Testing Guide

This manual documents the seed data loaded into the `order-management-system`
stack, plus ready-to-use sample payloads for testing each microservice
individually and end-to-end through Postman (or curl/PowerShell).

See [how to run the application.md](how%20to%20run%20the%20application.md) to
start the stack before using this guide.

## 1. Two known bugs were fixed to make the saga work

While seeding data for this guide we found and fixed three real bugs in the
codebase (source already patched — no action needed, just context for why
things behave the way described below):

1. **`@PathVariable`/`@RequestParam` without an explicit name threw 500s** —
   the root `pom.xml` didn't set `maven.compiler.parameters=true`, so the
   compiler dropped parameter names and Spring couldn't bind them on Java 21.
   Fixed in `order-management-system/pom.xml`.
2. **`order-service` couldn't reserve stock** — Feign's default HTTP client
   can't send `PATCH`. Fixed by adding `io.github.openfeign:feign-hc5` to
   `order-service`'s `pom.xml`.
3. **Orders never left `CREATED` status, and no "payment confirmed/declined"
   notification ever appeared** — `PaymentEventListener`/
   `NotificationEventListener` consumed the `payment-events` Kafka topic with
   a listener method typed `Object event`. Spring Kafka treats an
   `Object`-typed parameter as eligible for the *raw* `ConsumerRecord` (since
   `Object` is assignable from it) and skips payload deserialization
   entirely — silently, with no exception and no lag, so it looked like
   nothing was wrong. Fixed by switching to the documented
   class-level-`@KafkaListener` + per-type-`@KafkaHandler` pattern in
   `order-service`'s `PaymentEventListener` and a new
   `notification-service`'s `PaymentEventNotificationListener`.

If you ever see orders stuck in `CREATED` forever with correct rows in
`/api/payments`, that's this exact bug regressing — check for a
`@KafkaListener` method with a plain `Object` parameter.

## 2. Loading seed data

All services use **in-memory H2 databases** — data is lost on every
`docker compose down` / restart. Reload seed data any time with:

```powershell
Set-Location "d:\downloads\TRAINING\Project\order-management-system"
powershell -ExecutionPolicy Bypass -File .\scripts\seed-data.ps1
```

The script logs in as `admin`, then creates 5 customers, 9 products and 4
orders through the API Gateway (`http://localhost:8080`) — driving the full
Kafka saga (order → payment → notification) — and prints every generated ID.

## 3. Authentication (required for the API Gateway)

Every gateway route except `/api/auth/login`, `/actuator`, and the Swagger/API
docs paths requires a JWT bearer token.

**POST** `http://localhost:8080/api/auth/login`
```json
{ "username": "admin", "password": "admin123" }
```
Response:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer" }
```
Other demo user: `customer` / `customer123` (role `CUSTOMER` vs `ADMIN` — the
demo app doesn't currently restrict endpoints by role, both work identically).

Add `Authorization: Bearer <token>` to every subsequent gateway request in
Postman (set it once on the collection/environment level).

> Calling a service **directly** on its own port (8081–8085) skips the
> gateway and does **not** require a token — useful for isolating a single
> service, but not how a real client would call it.

## 4. Seed data reference (IDs from a fresh `docker compose up` + seed run)

### Pre-loaded by each service's own `data.sql` (exists before the seed script runs)
| Service | ID | Data |
|---|---|---|
| customer-service | 1 | Alice Johnson, alice@example.com, 555-0100, 12 Baker Street |
| customer-service | 2 | Bob Smith, bob@example.com, 555-0101, 99 High Street |
| product-service | 1 | Wireless Mouse, $19.99, stock 100 |
| product-service | 2 | Mechanical Keyboard, $59.99, stock 50 |
| product-service | 3 | 27-inch Monitor, $249.99, stock 25 |

### Customers created by `seed-data.ps1`
| id | name | email | phone |
|---|---|---|---|
| 3 | John Doe | john.doe@example.com | 555-0101 |
| 4 | Jane Smith | jane.smith@example.com | 555-0102 |
| 5 | Carlos Diaz | carlos.diaz@example.com | 555-0103 |
| 6 | Aisha Khan | aisha.khan@example.com | 555-0104 |
| 7 | Wei Chen | wei.chen@example.com | 555-0105 |

### Products created by `seed-data.ps1`
| id | name | price | stock |
|---|---|---|---|
| 4 | Wireless Mouse | 19.99 | 200 |
| 5 | Mechanical Keyboard | 89.99 | 150 |
| 6 | 27-inch 4K Monitor | 349.99 | 60 |
| 7 | USB-C Docking Station | 129.50 | 100 |
| 8 | Noise Cancelling Headphones | 249.00 | 80 |
| 9 | Laptop Stand | 45.00 | 120 |
| 10 | Premium Ultrabook | 1499.00 | 15 |
| 11 | 1TB External SSD | 109.99 | 90 |
| 12 | Low Stock Webcam | 39.99 | 2 *(kept low on purpose for stock-conflict tests)* |

### Orders created by `seed-data.ps1` (drives the full saga)
| id | customer | items | total | payment | final order status |
|---|---|---|---|---|---|
| 1 | 3 (John Doe) | 2× Wireless Mouse (#4), 1× Mechanical Keyboard (#5) | 129.97 | APPROVED | **PAID** |
| 2 | 4 (Jane Smith) | 1× Premium Ultrabook (#10) | 1499.00 | **DECLINED** (over $1000 auto-approval threshold) | **FAILED** |
| 3 | 5 (Carlos Diaz) | 1× 27-inch 4K Monitor (#6), 1× Noise Cancelling Headphones (#8) | 598.99 | APPROVED | **PAID** |
| 4 | 6 (Aisha Khan) | 1× Laptop Stand (#9) | 45.00 | APPROVED | **PAID** |

> **Note:** stock is deducted at order-creation time regardless of the later
> payment outcome — order #2 was declined, but product #10's stock still
> dropped from 15 to 14. There's no compensating "release stock" step in this
> demo saga (see the comment in `OrderService`/`PaymentEventListener`), so
> that's expected behavior, not a bug.

### Resulting payments (`GET /api/payments`)
| id | orderId | amount | status | declineReason |
|---|---|---|---|---|
| 1 | 1 | 129.97 | APPROVED | — |
| 2 | 2 | 1499.00 | DECLINED | Amount exceeds auto-approval threshold of 1000.00 |
| 3 | 3 | 598.99 | APPROVED | — |
| 4 | 4 | 45.00 | APPROVED | — |

### Resulting notifications (`GET http://localhost:8085/api/notifications` — **not** routed through the gateway)
Each order produces two notifications: one on creation (`order-events`) and
one after payment settles (`payment-events`):
```
order #1: "Your order #1 has been received and totals 129.97"
order #1: "Payment confirmed for order #1. Thank you for your purchase!"
order #2: "Your order #2 has been received and totals 1499.00"
order #2: "Payment for order #2 could not be processed: Amount exceeds auto-approval threshold of 1000.00"
order #3: "Your order #3 has been received and totals 598.99"
order #3: "Payment confirmed for order #3. Thank you for your purchase!"
order #4: "Your order #4 has been received and totals 45.00"
order #4: "Payment confirmed for order #4. Thank you for your purchase!"
```

## 5. Postman setup

Create an environment with two variables:
- `baseUrl` = `http://localhost:8080`
- `token` = *(paste the value from the login response)*

Set the collection's Authorization tab to **Bearer Token** using
`{{token}}` so every request inherits it automatically.

## 6. Per-service manual test cases

### 6.1 customer-service (`/api/customers`, via gateway or directly on `:8081`)

**Create — 201 Created**
```http
POST {{baseUrl}}/api/customers
{
  "name": "Priya Nair",
  "email": "priya.nair@example.com",
  "phone": "555-0199",
  "address": "5 Marine Drive, Mumbai"
}
```

**Get by id — 200 OK**
```http
GET {{baseUrl}}/api/customers/3
```

**List all — 200 OK**
```http
GET {{baseUrl}}/api/customers
```

**Update — 200 OK**
```http
PUT {{baseUrl}}/api/customers/3
{ "name": "John Doe Jr.", "email": "john.doe@example.com", "phone": "555-9999", "address": "New Address" }
```

**Delete — 204 No Content**
```http
DELETE {{baseUrl}}/api/customers/7
```

**Negative — 404 Not Found**
```http
GET {{baseUrl}}/api/customers/9999
```

**Negative — 400 Bad Request (validation)**
```http
POST {{baseUrl}}/api/customers
{ "name": "", "email": "not-an-email" }
```
Expect `ApiError` with `details: ["name: name is required", "email: email must be valid"]`.

**Negative — 409 Conflict (duplicate email)**
```http
POST {{baseUrl}}/api/customers
{ "name": "Duplicate Alice", "email": "alice@example.com", "phone": "000", "address": "x" }
```

### 6.2 product-service (`/api/products`, via gateway or directly on `:8082`)

**Create — 201 Created**
```http
POST {{baseUrl}}/api/products
{ "name": "Bluetooth Speaker", "description": "Portable waterproof speaker", "price": 59.99, "stockQuantity": 75 }
```

**Get by id / List all**
```http
GET {{baseUrl}}/api/products/6
GET {{baseUrl}}/api/products
```

**Reserve stock — 200 OK** (used internally by order-service, but callable directly)
```http
PATCH {{baseUrl}}/api/products/9/reserve-stock
{ "quantity": 3 }
```

**Negative — 409 Conflict (insufficient stock)** — use the low-stock product #12 (only 2 in stock)
```http
PATCH {{baseUrl}}/api/products/12/reserve-stock
{ "quantity": 10 }
```

**Negative — 400 Bad Request (validation)**
```http
POST {{baseUrl}}/api/products
{ "name": "", "price": -5, "stockQuantity": -1 }
```

**Negative — 404 Not Found**
```http
GET {{baseUrl}}/api/products/9999
```

### 6.3 order-service (`/api/orders`, via gateway or directly on `:8083`)

**Create — 201 Created (approved path, total ≤ $1000)**
```http
POST {{baseUrl}}/api/orders
{
  "customerId": 5,
  "items": [
    { "productId": 4, "quantity": 1 },
    { "productId": 11, "quantity": 1 }
  ]
}
```

**Create — 201 Created (declined path, total > $1000)**
```http
POST {{baseUrl}}/api/orders
{ "customerId": 6, "items": [ { "productId": 10, "quantity": 1 } ] }
```
Poll `GET {{baseUrl}}/api/orders/{id}` a few seconds later — status flips
from `CREATED` → `FAILED`. Check `GET {{baseUrl}}/api/payments` for the
decline reason and `GET http://localhost:8085/api/notifications` for the
customer-facing message.

**Get by id / list all / list by customer**
```http
GET {{baseUrl}}/api/orders/1
GET {{baseUrl}}/api/orders
GET {{baseUrl}}/api/orders/customer/3
```

**Negative — 404 Not Found (unknown customer)**
```http
POST {{baseUrl}}/api/orders
{ "customerId": 9999, "items": [ { "productId": 4, "quantity": 1 } ] }
```

**Negative — 404/409 (unknown product / insufficient stock)**
```http
POST {{baseUrl}}/api/orders
{ "customerId": 3, "items": [ { "productId": 12, "quantity": 50 } ] }
```

**Negative — 400 Bad Request (empty items)**
```http
POST {{baseUrl}}/api/orders
{ "customerId": 3, "items": [] }
```

### 6.4 payment-service (`/api/payments`, read-only — via gateway or directly on `:8084`)
Payments are created only as a side effect of order creation (choreographed
Saga via Kafka), not via direct POST.
```http
GET {{baseUrl}}/api/payments
```

### 6.5 notification-service (`/api/notifications`, read-only — **not** routed through the gateway, call `:8085` directly)
```http
GET http://localhost:8085/api/notifications
```

## 7. Full integration test scenarios (multi-service, via Postman)

**Scenario A — Happy path saga**
1. `POST /api/auth/login` → capture `token`.
2. `POST /api/customers` → capture new `customerId`.
3. `POST /api/products` → capture new `productId` with a price ≤ $1000.
4. `POST /api/orders` with that `customerId`/`productId` → capture `orderId`, expect `status: CREATED`.
5. Wait ~3–5 seconds (Kafka saga settling time).
6. `GET /api/orders/{orderId}` → expect `status: PAID`.
7. `GET /api/payments` → find the row for `orderId`, expect `status: APPROVED`.
8. `GET http://localhost:8085/api/notifications` → expect a "Payment confirmed" message for `orderId`.

**Scenario B — Declined payment (compensating path)**
Same as Scenario A but use/create a product priced **over $1000** (e.g.
product #10, Premium Ultrabook). Expect final order `status: FAILED`,
payment `status: DECLINED`, and a "could not be processed" notification.
Also re-check the product's `stockQuantity` — it will still be decremented
(known limitation, see §1/§4 note — no compensating stock release).

**Scenario C — Downstream failure (circuit breaker)**
1. Stop customer-service: `docker compose stop customer-service`.
2. `POST /api/orders` referencing any existing customer.
3. Expect `503 Service Unavailable` (`DownstreamServiceException` /
   Resilience4j circuit breaker opens after repeated failures — see
   `resilience4j.circuitbreaker.instances.customerService` in
   `order-service/application.yml`).
4. Restart it: `docker compose start customer-service`, wait ~45s for it to
   re-register with Eureka, then retry — should succeed again.

## 8. Extra sample data for more manual testing

Extra customers:
```json
[
  { "name": "Diego Ramirez", "email": "diego.ramirez@example.com", "phone": "555-0210", "address": "8 Rue de Rivoli, Paris" },
  { "name": "Fatima Al-Sayed", "email": "fatima.alsayed@example.com", "phone": "555-0211", "address": "22 Corniche Rd, Doha" },
  { "name": "Hiroshi Tanaka", "email": "hiroshi.tanaka@example.com", "phone": "555-0212", "address": "3-1 Shibuya, Tokyo" },
  { "name": "Emma Wilson", "email": "emma.wilson@example.com", "phone": "555-0213", "address": "10 Downing St, London" }
]
```

Extra products (mix of normal + edge-case prices):
```json
[
  { "name": "Gaming Chair", "description": "Ergonomic racing-style chair", "price": 249.99, "stockQuantity": 40 },
  { "name": "4K Webcam", "description": "Ultra HD webcam with autofocus", "price": 79.99, "stockQuantity": 60 },
  { "name": "Portable SSD 2TB", "description": "USB-C NVMe enclosure", "price": 189.99, "stockQuantity": 45 },
  { "name": "Server Rack Workstation", "description": "High-end workstation tower", "price": 2499.00, "stockQuantity": 5 },
  { "name": "Cable Clip Pack", "description": "10-pack cable organizers", "price": 4.99, "stockQuantity": 500 },
  { "name": "Zero Stock Item", "description": "For testing 409 insufficient stock", "price": 9.99, "stockQuantity": 0 }
]
```

Extra order combinations to try:
```json
// Multi-line order, comfortably under threshold
{ "customerId": 3, "items": [ { "productId": 4, "quantity": 3 }, { "productId": 7, "quantity": 1 }, { "productId": 9, "quantity": 2 } ] }

// Exactly at the $1000.00 threshold (should still be APPROVED — rule is "<=")
{ "customerId": 4, "items": [ { "productId": 5, "quantity": 1 }, { "productId": 6, "quantity": 1 }, { "productId": 8, "quantity": 1 }, { "productId": 11, "quantity": 3 } ] }

// $0.01 over the threshold (should be DECLINED)
{ "customerId": 5, "items": [ { "productId": 10, "quantity": 1 } ] }

// Zero-stock product (expect order creation to fail with 409/500 from product-service)
{ "customerId": 6, "items": [ { "productId": 12, "quantity": 1 } ] }
```

## 9. Troubleshooting

- **All data disappeared** — expected after `docker compose down`/restart
  (in-memory H2). Re-run `scripts/seed-data.ps1`.
- **401 Unauthorized on any `/api/...` gateway call** — token missing/expired.
  Re-run the login request; tokens are short-lived JWTs (see
  `JwtService` in `api-gateway`).
- **404 from the gateway on `/api/notifications`** — that endpoint isn't
  registered as a gateway route; call `http://localhost:8085/api/notifications`
  directly.
- **Order stuck on `CREATED` forever** — see the Kafka listener bug in §1;
  confirm you're running the latest images (`docker compose up -d --build`
  after any source change).
- **`503`/circuit breaker errors right after a restart** — Eureka client
  caches take up to ~30s to refresh after a dependent service restarts; wait
  and retry before assuming something is broken.
