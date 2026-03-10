# Design a Webhook Delivery System
**Shopify delivers billions of webhooks to merchants — reliable, at-least-once delivery**

---

## 1. Requirements (~3 min)

### Functional Requirements
1. Shopify app events trigger webhook delivery to merchant endpoints
2. Merchants subscribe to topics (e.g., "orders/create") with a callback URL
3. **At-least-once delivery** — every webhook is delivered or dead-lettered
4. Exponential backoff retry on failure; auto-disable broken endpoints

### Non-Functional Requirements
1. **~1B webhooks/day** → ~12K/sec avg, spikes to ~50K/sec (BFCM)
2. **Best-effort latency** — deliver within seconds, eventual delivery acceptable
3. No strict ordering required (idempotent delivery)
4. Small JSON payloads (~1-10 KB)

---

## 2. Core Entities (~2 min)

- **WebhookSubscription** — merchant's registration for a topic + callback URL
- **WebhookDelivery** — a single delivery attempt record (the main state-driven entity)

---

## 3. API (~3 min)

```
# Merchant API
POST   /v1/webhooks                    → subscribe to a topic
GET    /v1/webhooks                    → list subscriptions
DELETE /v1/webhooks/:id                → unsubscribe
GET    /v1/webhooks/:id/deliveries     → delivery history + status
POST   /v1/webhooks/:id/test           → send a test payload

# Internal (Producer)
POST   /v1/internal/webhooks/enqueue   → enqueue delivery for (shop_id, topic, payload)
```

---

## 4. High-Level Architecture

**Single monolithic app — no microservices.** One app server handles subscription management and enqueue. Background workers poll the deliveries table directly for work.

```
┌──────────────────────────────────────────────────┐
│                 Client Layer                      │
│                                                  │
│   ┌──────────────┐       ┌───────────────────┐   │
│   │ Shopify App   │       │  Merchant Portal  │   │
│   │ (Producer)    │       │  (manage subs)    │   │
│   └──────┬───────┘       └───────┬───────────┘   │
└──────────┼───────────────────────┼───────────────┘
           │                       │
           ▼                       ▼
┌──────────────────────────────────────────────────┐
│           Service Application Layer               │
│                                                  │
│  ┌────────────────┐  ┌───────────────────────┐   │
│  │  Enqueue API   │  │  Subscription API     │   │
│  │  (internal)    │  │  (merchant-facing)    │   │
│  └────────────────┘  └───────────────────────┘   │
│                                                  │
│  Callout:                                        │
│  insert delivery row (status=PENDING)            │
│  in same transaction as business event           │
│                                                  │
│  Requirements:                                   │
│  * no lost webhooks (same-txn insert)            │
│  * workers poll deliveries.status directly       │
│  * idempotent via unique delivery_id             │
└──────────────────┬───────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────┐
│                  Data Layer                       │
│                                                  │
│  ┌──────────────────────┐  ┌─────────────────┐   │
│  │     Postgres DB       │  │  (no Redis/     │   │
│  │                       │  │   Kafka needed  │   │
│  │  * subscriptions      │  │   at start)     │   │
│  │  * deliveries         │  │                 │   │
│  │    (state-driven)     │  │                 │   │
│  └──────────────────────┘  └─────────────────┘   │
└──────────────────┬───────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────┐
│              Async System Layer                   │
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │ Delivery Worker                            │  │
│  │                                            │  │
│  │ * poll deliveries WHERE                    │  │
│  │   status IN ('pending','failed')           │  │
│  │   AND next_retry_at <= NOW()               │  │
│  │ * claim row: set status = IN_FLIGHT        │  │
│  │ * POST to merchant endpoint (HMAC signed)  │  │
│  │ * on 2xx: status = DELIVERED               │  │
│  │ * on failure: exponential backoff retry     │  │
│  │ * after 19 attempts: status = DEAD         │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │ Sweeper Worker (Reaper)                    │  │
│  │                                            │  │
│  │ * find stuck deliveries                    │  │
│  │   (IN_FLIGHT for > 5 min)                  │  │
│  │ * reset status = PENDING                   │  │
│  │ * find DEAD deliveries → check             │  │
│  │   subscription failure_count               │  │
│  │ * if threshold exceeded: disable endpoint  │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │ Notification Worker                        │  │
│  │                                            │  │
│  │ * poll subscriptions WHERE                 │  │
│  │   active=false AND notified_at IS NULL     │  │
│  │ * send email to merchant:                  │  │
│  │   "Your webhook endpoint was disabled"     │  │
│  │ * set notified_at                          │  │
│  └────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

---

## 5. Table Schemas

```
┌──────────────────────────────────────────┐
│ table : webhook_subscriptions            │
│                                          │
│ subscription_id (PK)                     │
│ shop_id                                  │
│ topic (e.g. "orders/create")             │
│ endpoint_url                             │
│ secret (HMAC signing secret)             │
│ active (boolean, default true)           │
│ failure_count (default 0)                │
│ disabled_at (nullable)                   │
│ notified_at (nullable)                   │
│ created_at                               │
│ updated_at                               │
│                                          │
│ UNIQUE (shop_id, topic, endpoint_url)    │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│ table : webhook_deliveries               │
│ (the main state-driven entity)           │
│                                          │
│ delivery_id (PK)                         │
│ subscription_id (FK)                     │
│ shop_id                                  │
│ topic                                    │
│ payload (JSONB)                          │
│ status                                   │
│ (pending/in_flight/delivered/failed/dead) │
│ attempt_count (default 0)                │
│ max_attempts (default 19)                │
│ next_retry_at                            │
│ last_http_status (nullable)              │
│ last_error (nullable)                    │
│ created_at                               │
│ updated_at                               │
│                                          │
│ INDEX (status, next_retry_at)            │
│ — workers poll this index directly       │
└──────────────────────────────────────────┘
```

Callout: **no separate job queue table needed.** The deliveries table IS the queue. Workers poll `status` + `next_retry_at` directly. `FOR UPDATE SKIP LOCKED` enables parallel workers with no conflicts.

---

## 6. Key Flows

### Flow 1: Sync — Enqueue (low latency, same transaction)

```
Shopify app event fires (e.g., order created)
    │
    ▼
Enqueue API handler:
    1. Look up all active subscriptions for (shop_id, topic)
    2. For each subscription:
       INSERT INTO webhook_deliveries
         (subscription_id, shop_id, topic, payload, status, next_retry_at)
       VALUES (..., 'pending', NOW())
    3. Commit in SAME transaction as the business event
    │
    ▼
Return immediately (fire-and-forget from producer's perspective)
```

Callout: same-transaction insert guarantees **no lost webhooks**. Even if workers are down, deliveries persist in the DB and drain when workers recover.

### Flow 2: Async — Delivery Worker

```
Delivery Worker (polls every 1-2s):
    │
    ▼
    1. Claim a ready delivery:
       UPDATE webhook_deliveries
       SET status = 'in_flight', updated_at = NOW()
       WHERE id = (
           SELECT id FROM webhook_deliveries
           WHERE status IN ('pending', 'failed')
             AND next_retry_at <= NOW()
           ORDER BY next_retry_at
           LIMIT 1
           FOR UPDATE SKIP LOCKED
       )
       RETURNING *;
    │
    ▼
    2. Build + sign HTTP POST:
       - JSON payload in body
       - X-Shopify-Hmac-SHA256: HMAC(body, subscription.secret)
       - X-Shopify-Webhook-Id: delivery_id (for merchant idempotency)
       - Timeout: 5s
    │
    ▼
    3. POST to merchant endpoint_url
    │
    ├── 2xx response:
    │   status = 'delivered'
    │   subscription.failure_count = 0 (reset on success)
    │
    └── non-2xx / timeout:
        attempt_count += 1
        if attempt_count >= max_attempts (19):
            status = 'dead'
            subscription.failure_count += 1
        else:
            status = 'failed'
            next_retry_at = NOW() + min(2^attempt_count * 30s, 24h)
```

### Flow 3: Async — Sweeper Worker (Reaper)

```
Every 60 seconds:
    1. Reclaim stuck in-flight deliveries:
       UPDATE webhook_deliveries SET status = 'pending'
       WHERE status = 'in_flight'
         AND updated_at < NOW() - INTERVAL '5 minutes';

    2. Check subscriptions for auto-disable:
       UPDATE webhook_subscriptions
       SET active = false, disabled_at = NOW()
       WHERE failure_count >= 10
         AND active = true;
```

Callout: handlers worker crashes between claiming and delivering — sweeper resets the row. **No delivery is ever permanently lost.**

---

## 7. State Machine

```
webhook_deliveries.status:

  PENDING ──────▶ IN_FLIGHT ──────▶ DELIVERED  (terminal)
     ▲               │
     │               │ (failure, attempt < max)
     │               ▼
     │            FAILED ──────────▶ IN_FLIGHT  (retry when next_retry_at reached)
     │               │
     │               │ (attempt >= max)
     │               ▼
     │             DEAD  (terminal, endpoint broken)
     │
     └── sweeper resets stuck IN_FLIGHT back to PENDING
```

### Backoff schedule

| Attempt | Delay       |
|---------|-------------|
| 1       | 1 min       |
| 2       | 2 min       |
| 3       | 4 min       |
| 5       | 16 min      |
| 10      | ~8.5 hrs    |
| 19      | ~24 hrs (cap) |

Total retry window: ~48 hours before dead-lettering.

---

## 8. Deep Dives

### Deep Dive 1: Security

- **HMAC signing**: Every payload signed with merchant-specific `secret` → merchant verifies authenticity
- **HTTPS only**: Reject `http://` endpoint URLs at subscription time
- **Timeout & size limits**: 5s connection timeout, 10KB max response read → prevent slow-loris
- **IP allowlist**: Publish Shopify's egress IPs so merchants can firewall
- **Secret rotation**: Support rotating HMAC secrets with a grace period (accept old + new)

### Deep Dive 2: Scaling

**Why PostgreSQL works at this scale:**
- `FOR UPDATE SKIP LOCKED` turns Postgres into a high-performance job queue
- At 12K/sec with ~50-100 worker threads, each delivery ~100-500ms → throughput achievable
- Partition `webhook_deliveries` by `created_at` (monthly), drop old partitions

**When to add Redis/Kafka:**
- If polling latency becomes an issue → add Redis Streams as a "wake up" notification layer
- Producer pushes delivery_id to a Redis stream, workers consume from stream instead of polling
- Postgres remains source of truth; Redis is just the fast signal
- Only add when polling contention becomes measurable problem

### Deep Dive 3: Observability

- **Metrics**: delivery success rate, p50/p99 latency, retry rate, dead letter rate, queue depth
- **Alerting**: queue depth growing (workers falling behind), high failure rate for specific merchants
- **Merchant dashboard**: show delivery history, response codes, allow manual retry of `dead` webhooks

---

## 9. Summary

| Concern | Solution |
|---------|----------|
| No lost webhooks | Same-transaction insert in Postgres |
| At-least-once | Retry until delivered or dead-lettered |
| Parallel workers | `FOR UPDATE SKIP LOCKED` on deliveries table |
| No separate queue | Workers poll `deliveries.status` directly |
| Stuck recovery | Sweeper resets stale `in_flight` → `pending` |
| Backoff | Exponential with 24h cap, 19 attempts |
| Bad endpoints | Auto-disable after threshold; notify merchant |
| Security | HMAC-SHA256 signing, HTTPS only |
| Scale path | More workers → Redis notification → partition/shard |

Callout: The main design choice is **no separate job queue**. The 1:1 relationship between event → delivery makes the deliveries table itself the queue. Workers poll `status` directly. This mirrors the pattern used in the Library Book Return design where workers poll `transactions.state` — simple, durable, no job loss.


