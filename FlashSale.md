---

# Design: Flash Sale / High-Traffic Sale System (40 min)

## 1. Requirements (~5 min)

### Functional Requirements
1. Merchant creates a flash sale (product, discounted price, quantity, time window)
2. Customers browse flash sale products
3. Customers add to cart → checkout → purchase
4. **No overselling** — inventory must be consistent

### Non-Functional Requirements
1. Handle **10K+ concurrent requests/sec** at sale start (100x normal traffic)
2. Product page latency **< 200ms** (read-heavy, cacheable)
3. Checkout **strong consistency** — no overselling even under race conditions
4. **Fair access** — first-come-first-served via virtual queue
5. **Graceful degradation** — shed excess traffic rather than crash

---

## 2. Core Entities (~2 min)

- **Product** — the item being sold
- **FlashSale** — event config (product, price, quantity, time window)
- **InventoryReservation** — temporary hold on inventory (TTL-based)
- **Order** — completed purchase

---

## 3. API (~5 min)

```
# Merchant API
POST   /v1/flash-sales                    → create sale event
GET    /v1/flash-sales/:saleId            → get sale details

# Customer API
GET    /v1/flash-sales/:saleId/product    → view product + availability
POST   /v1/flash-sales/:saleId/reserve    → reserve inventory (add to cart)
POST   /v1/flash-sales/:saleId/checkout   → purchase (idempotency_key in header)
DELETE /v1/flash-sales/:saleId/reserve    → release reservation

# Internal
GET    /v1/flash-sales/:saleId/status     → remaining qty (for countdown UI)
```

---

## 4. Data Schema (3 tables + Redis)

```
┌─────────────────────────────────┐
│ table: flash_sales              │
│                                 │
│ sale_id (PK)                    │
│ product_id (FK)                 │
│ merchant_id (FK)                │
│ sale_price                      │
│ total_quantity                  │
│ status (scheduled/active/ended) │
│ starts_at                       │
│ ends_at                         │
│ created_at                      │
└─────────────────────────────────┘

┌─────────────────────────────────────┐
│ table: inventory_reservations       │
│                                     │
│ reservation_id (PK)                 │
│ sale_id (FK)                        │
│ user_id (FK)                        │
│ quantity                            │
│ status (reserved/purchased/expired) │
│ reserved_at                         │
│ expires_at (TTL, e.g. 10 min)       │
└─────────────────────────────────────┘

┌──────────────────────────────────┐
│ table: orders                    │
│                                  │
│ order_id (PK)                    │
│ idempotency_key (UNIQUE)         │
│ sale_id (FK)                     │
│ user_id (FK)                     │
│ quantity                         │
│ total_price                      │
│ payment_status                   │
│ (pending/charged/failed/refunded)│
│ created_at                       │
└──────────────────────────────────┘

Redis:
  flash_sale:{sale_id}:remaining → atomic integer counter
  flash_sale:{sale_id}:queue     → sorted set (virtual queue)
```

---

## 5. High-Level Architecture

**Single monolithic app — no microservices.** One app server handles product reads, inventory reservation, and checkout. Background worker threads handle reservation expiry and sale lifecycle.

```
                        ┌──────────┐
                        │   CDN    │ ← static assets, product images
                        └────┬─────┘
                             │
    ┌────────────────────────▼───────────────────────┐
    │              API Gateway / Load Balancer        │
    │   (Auth, Rate Limit per user, Queue Gate)       │
    └────────────────────┬────────────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │    App Server        │  (stateless, horizontally scaled)
              │                     │
              │  Routes:             │
              │  • GET  /product     │  → Redis cache → Postgres
              │  • POST /reserve     │  → Redis DECR  → Postgres
              │  • POST /checkout    │  → Postgres    → Payment Gateway
              │  • DELETE /reserve   │  → Redis INCR  → Postgres
              │                     │
              │  Background Workers: │
              │  • Reservation expiry│  (every 30s, expire stale holds)
              │  • Sale lifecycle    │  (activate/deactivate sales)
              └──┬──────┬───────┬───┘
                 │      │       │
                 ▼      ▼       ▼
          ┌────────┐ ┌──────┐ ┌──────────────┐
          │ Redis  │ │Postgres│ │ Payment      │
          │        │ │       │ │ Gateway      │
          │• cache │ │• urls │ │ (Stripe/etc) │
          │• counter│ │• orders│ │              │
          │• queue │ │• resv │ │              │
          └────────┘ └──────┘ └──────────────┘
```

**Why no microservices?**
- Flash sale logic is tightly coupled (reserve → checkout → payment)
- A single deploy unit is simpler to reason about, deploy, and debug
- Horizontal scaling is done by adding more app server instances (stateless)
- At Shopify's scale, a monolith + Redis + Postgres handles 10K/sec fine
```

---

## 6. Key Flows

### Flow 1: Reserve Inventory (the critical path)

```
Customer clicks "Buy Now"
    │
    ▼
API Gateway (rate limit: 10 req/sec/user)
    │
    ▼
App Server → POST /reserve handler:
    1. DECR flash_sale:{sale_id}:remaining in Redis
       → if result >= 0: reservation granted
       → if result < 0:  INCR back, return "SOLD OUT"
    2. Insert inventory_reservations row
       (status=reserved, expires_at=now+10min)
    3. Return reservation_id to client
```

**Why Redis DECR?** It's atomic, single-threaded, O(1). No locks, no contention. 10K concurrent DECR operations complete in milliseconds. This is the core trick — Redis is the **gatekeeper**, Postgres is the **source of truth**.

### Flow 2: Checkout (convert reservation to order)

```
Customer clicks "Pay"
    │
    ▼
App Server → POST /checkout handler:
    1. Validate reservation exists + not expired
    2. Create order (idempotency_key = dedup)
    3. Call Payment Gateway (charge card)
    4. On success:
       - order.payment_status = 'charged'
       - reservation.status = 'purchased'
    5. On failure:
       - order.payment_status = 'failed'
       - reservation.status = 'expired'
       - INCR Redis counter (return inventory)
```

### Flow 3: Reservation Expiry (background worker thread in app server)

```
Every 30 seconds:
    SELECT * FROM inventory_reservations
    WHERE status = 'reserved' AND expires_at < now()
    FOR UPDATE SKIP LOCKED;

    For each expired reservation:
      - SET status = 'expired'
      - INCR flash_sale:{sale_id}:remaining in Redis
      (inventory returns to pool for others)
```

Runs as a scheduled thread inside the app server (e.g., @Scheduled in Spring,
cron job in Rails). Only one instance should run this — use a Postgres advisory
lock or a Redis SETNX lock to prevent duplicate execution across instances.

---

## 7. Deep Dives (~10 min)

### Deep Dive 1: Preventing Overselling

**Problem:** What if Redis crashes or drifts from Postgres?

**Solution: Redis is the fast path, Postgres is the source of truth.**

```
Redis counter = total_quantity
             - COUNT(reservations WHERE status='reserved')
             - COUNT(orders WHERE payment_status='charged')
```

If Redis crashes:
1. Sale pauses (API returns "temporarily unavailable")
2. Rebuild counter from Postgres
3. Resume

**Double-check on checkout:**
```sql
-- Before charging payment, verify in Postgres:
SELECT COUNT(*) FROM orders
WHERE sale_id = ? AND payment_status = 'charged';
-- Must be < total_quantity
```

### Deep Dive 2: Handling the Traffic Spike

**Layer 1: CDN** — Product pages are mostly static. Cache aggressively. Only the "remaining quantity" widget needs real-time data.

**Layer 2: API Gateway Queue Gate** — When sale starts, if requests > threshold:
- Assign users a queue position (Redis sorted set, score = arrival timestamp)
- Return "You're #1,234 in line" + polling endpoint
- Admit users in batches (e.g., 100/sec)
- This converts a 10K/sec spike into a smooth 100/sec flow

```
POST /v1/flash-sales/:saleId/enter-queue
→ { position: 1234, estimatedWaitSeconds: 12 }

GET /v1/flash-sales/:saleId/queue-status
→ { admitted: true } or { position: 456 }
```

**Layer 3: Rate Limiting** — Per-user rate limit (10 req/sec) prevents bots from hoarding.

### Deep Dive 3: Idempotency

**Problem:** Customer double-clicks "Pay", or network retry sends checkout twice.

**Solution:** `idempotency_key` (UUID generated client-side) as UNIQUE constraint on orders table.
- First request: creates order, charges card
- Retry: finds existing order by idempotency_key, returns same result
- No double-charge

### Deep Dive 4: What if Payment Gateway is slow?

**Problem:** Stripe takes 3 seconds to respond. Customer waits.

**Solution:** Async checkout with optimistic response:
1. Reserve inventory (fast, Redis)
2. Return "Order pending, we'll confirm shortly"
3. Checkout worker processes payment async
4. Push notification / WebSocket to client with result

This only matters at extreme scale. For most Shopify merchants, synchronous checkout is fine (Stripe P99 < 2s).

---

## 8. Metrics & Monitoring (bonus callout)

| Metric | Alert Threshold |
|---|---|
| Redis counter value | < 5% remaining → "almost sold out" event |
| Reservation expiry rate | > 50% → customers abandoning, shorten TTL |
| Checkout success rate | < 95% → payment gateway issue |
| API latency P99 | > 500ms → scaling issue |
| Queue depth | > 10K → need more admission capacity |

---

## 9. Trade-offs / Cons

| Trade-off | Consequence |
|---|---|
| Redis as gatekeeper | If Redis crashes, sale pauses until rebuilt from Postgres |
| 10-min reservation TTL | Too short: customers can't finish checkout. Too long: inventory held by abandoners |
| Virtual queue | Adds UX complexity; some customers hate waiting |
| Async checkout | Customer uncertainty ("did it go through?") |

---

## 10. Presenter Notes (40-min pacing)

| Section | Time | Focus |
|---|---|---|
| Requirements | 5 min | Emphasize 10K/sec spike + no overselling |
| Entities + API | 5 min | Quick, show idempotency_key |
| Schema | 3 min | 3 tables + Redis counter — keep simple |
| Architecture | 7 min | Draw the diagram, explain CDN → Gateway → App Server → Redis → Postgres |
| Key Flows | 8 min | Reserve flow is the star — show Redis DECR trick |
| Deep Dive: Overselling | 4 min | Redis fast path + Postgres source of truth |
| Deep Dive: Traffic Spike | 4 min | Queue gate pattern |
| Deep Dive: Idempotency | 2 min | Quick, you've done this before |
| Trade-offs | 2 min | Redis crash, TTL tuning |
