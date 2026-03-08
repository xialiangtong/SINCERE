# Design a Webhook Delivery System
**Shopify delivers billions of webhooks to merchants — reliable, at-least-once delivery**

---

## Problem Statement

Shopify delivers billions of webhooks to merchants. Design a system that reliably delivers webhooks with **at-least-once** semantics.

---

## 1. Clarifying Questions (2-3 min)

- **Scale?** ~1B webhooks/day → ~12K/sec avg, spikes to ~50K/sec (flash sales, BFCM)
- **Latency SLA?** Best-effort, deliver within seconds, but eventual delivery is acceptable
- **Retry policy?** Retry with exponential backoff; disable endpoint after repeated failures
- **Ordering?** No strict ordering required (at-least-once, not exactly-once)
- **Payload size?** Small JSON payloads (~1-10 KB)

---

## 2. High-Level Design (5 min)

```
┌──────────────┐      ┌─────────────────┐      ┌──────────────────┐
│  Shopify App  │─────▶│  Message Queue   │─────▶│  Delivery Worker │──▶ Merchant Endpoint
│  (Producer)   │      │  (PostgreSQL /   │      │  (Consumer Pool)  │
│               │      │   Redis Streams) │      │                  │
└──────────────┘      └─────────────────┘      └──────┬───────────┘
                                                       │
                                                       │ on failure
                                                       ▼
                                                ┌──────────────┐
                                                │  Retry Queue  │
                                                │  (Delayed)    │
                                                └──────────────┘
```

**Single service, three logical components:**
1. **Producer** — Shopify app events enqueue webhook jobs
2. **Queue** — Durable message queue holding pending deliveries
3. **Worker pool** — Consumers that POST to merchant URLs and handle retries

---

## 3. Data Model (3 min)

### `webhook_subscriptions` table
```sql
id              BIGINT PRIMARY KEY
shop_id         BIGINT NOT NULL
topic           VARCHAR(100)        -- e.g., "orders/create"
endpoint_url    VARCHAR(2048)
secret          VARCHAR(256)        -- HMAC signing secret
active          BOOLEAN DEFAULT TRUE
failure_count   INT DEFAULT 0
disabled_at     TIMESTAMP NULL
```

### `webhook_deliveries` table (the job queue)
```sql
id              BIGINT PRIMARY KEY
subscription_id BIGINT REFERENCES webhook_subscriptions(id)
topic           VARCHAR(100)
payload         JSONB
status          VARCHAR(20)         -- 'pending', 'in_flight', 'delivered', 'failed', 'dead'
attempt_count   INT DEFAULT 0
next_retry_at   TIMESTAMP
created_at      TIMESTAMP
updated_at      TIMESTAMP
```

**Index:** `(status, next_retry_at)` — for workers to pick up ready jobs efficiently.

---

## 4. Core Flow (10 min)

### 4a. Enqueue (Producer)

When an event fires (e.g., order created):

```
1. Look up all active subscriptions for (shop_id, topic)
2. For each subscription:
   INSERT INTO webhook_deliveries (subscription_id, topic, payload, status, next_retry_at)
   VALUES (..., 'pending', NOW())
```

This can be done **transactionally** with the business event (same DB) — guarantees no lost webhooks.

### 4b. Deliver (Worker)

Workers poll for ready jobs:

```sql
UPDATE webhook_deliveries
SET status = 'in_flight', updated_at = NOW()
WHERE id = (
    SELECT id FROM webhook_deliveries
    WHERE status IN ('pending', 'failed') AND next_retry_at <= NOW()
    ORDER BY next_retry_at
    LIMIT 1
    FOR UPDATE SKIP LOCKED    -- enables parallel workers without conflicts
)
RETURNING *;
```

Then:
```
1. Build HTTP POST request with JSON payload
2. Sign body with HMAC-SHA256 using subscription secret
   → Set header: X-Shopify-Hmac-SHA256: <signature>
3. POST to merchant endpoint_url (timeout: 5s)
4. If 2xx response:
     UPDATE status = 'delivered'
5. If non-2xx or timeout:
     → Retry logic (see below)
```

### 4c. Retry with Exponential Backoff

On failure:
```
attempt_count += 1
next_retry_at = NOW() + min(2^attempt_count * 30 seconds, 24 hours)

If attempt_count >= 19 (~48 hours total):
    status = 'dead'
    Increment subscription.failure_count
    If failure_count >= threshold (e.g., 10 consecutive):
        Mark subscription as disabled (active = false, disabled_at = NOW())
        Notify merchant via email/admin panel
```

**Backoff schedule example:**
| Attempt | Delay |
|---------|-------|
| 1 | 1 min |
| 2 | 2 min |
| 3 | 4 min |
| 5 | 16 min |
| 10 | ~8.5 hrs |
| 19 | ~24 hrs (capped) |

---

## 5. Reliability Guarantees (5 min)

### At-Least-Once Delivery
- Job is only marked `delivered` after a 2xx response
- If worker crashes mid-flight → `in_flight` jobs with stale `updated_at` are reclaimed by a reaper:
  ```sql
  UPDATE webhook_deliveries SET status = 'pending'
  WHERE status = 'in_flight' AND updated_at < NOW() - INTERVAL '5 minutes';
  ```
- Merchants must handle **idempotency** on their side (we include a unique `X-Shopify-Webhook-Id` header)

### No Lost Events
- Webhook row is inserted in the **same transaction** as the business event
- Even if workers are all down, jobs stay in the DB and drain when workers recover

### Poison Pill Protection
- Max attempts cap → jobs go to `dead` status, won't block the queue
- Bad endpoints get automatically disabled

---

## 6. Scaling (5 min)

### Why PostgreSQL works at Shopify scale

- `FOR UPDATE SKIP LOCKED` turns Postgres into a high-performance job queue
- At 12K/sec, with a pool of ~50-100 worker threads, each delivery takes ~100-500ms (network I/O), so throughput is achievable
- Partition `webhook_deliveries` by `created_at` (monthly), drop old partitions

### When to add Redis/Kafka

If polling latency becomes an issue:
- **Redis Streams** as a fast notification layer — producer pushes job ID to a stream, workers consume from stream instead of polling
- Postgres remains source of truth; Redis is just a "wake up" signal

### Horizontal scaling
- Add more worker processes — `SKIP LOCKED` ensures no double-processing
- Shard by `shop_id` if single Postgres becomes a bottleneck (but this is very far out)

---

## 7. Security (2 min)

- **HMAC signing**: Every payload signed with merchant-specific secret → merchant verifies authenticity
- **HTTPS only**: Reject `http://` endpoint URLs at subscription time
- **Timeout & size limits**: 5s connection timeout, 10KB max response read → prevent slow-loris / resource exhaustion
- **IP allowlist**: Publish Shopify's egress IPs so merchants can firewall
- **Secret rotation**: Support rotating HMAC secrets with a grace period (accept old + new)

---

## 8. Observability (2 min)

- **Metrics**: delivery success rate, p50/p99 latency, retry rate, dead letter rate, queue depth
- **Alerting**: queue depth growing (workers falling behind), high failure rate for specific merchants
- **Merchant dashboard**: show delivery history, response codes, allow manual retry of `dead` webhooks

---

## 9. API for Merchants

```
POST   /webhooks                  -- subscribe to a topic
GET    /webhooks                  -- list subscriptions
DELETE /webhooks/:id              -- unsubscribe
GET    /webhooks/:id/deliveries   -- delivery history + status
POST   /webhooks/:id/test         -- send a test payload
```

---

## Summary

| Concern | Solution |
|---------|----------|
| Durability | Same-transaction insert in Postgres |
| At-least-once | Retry until success or dead-letter |
| Parallel workers | `FOR UPDATE SKIP LOCKED` |
| Backoff | Exponential with 24h cap |
| Bad endpoints | Auto-disable after threshold |
| Security | HMAC-SHA256 signing, HTTPS only |
| Scale path | More workers → Redis notification → partition/shard |

**Key design choice:** A single Postgres-backed job queue avoids the complexity of Kafka/RabbitMQ for the interview scope, while still being production-viable at significant scale. This is exactly how many real systems (including parts of Shopify) have worked.


