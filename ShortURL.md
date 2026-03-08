# System design problem
Design a URL shortening service like TinyURL.
Users submit a long URL and receive a short URL. When someone visits the short URL, the system redirects them to the original long URL. The system should support very high read traffic, low redirect latency, collision-free short-code generation, link expiration or deletion if needed, and basic analytics such as click counts.

You should discuss the API, data model, code-generation strategy, storage, scaling, caching, rate limiting, abuse prevention, and trade-offs between simplicity and global uniqueness.

---

# Solution

## 1. Clarifying Questions & Requirements

| Question | Answer |
|----------|--------|
| Scale? | ~100M URLs created/month, ~10B redirects/month |
| Read:Write ratio? | ~100:1 (read-heavy) |
| Short code length? | 7 characters (base62) → 62^7 ≈ 3.5 trillion combinations |
| Custom aliases? | Nice-to-have, not core |
| Expiration? | Default 5 years, configurable |
| Analytics? | Basic click count, timestamp |

**Back-of-envelope:**
- Writes: ~100M/month ≈ ~40/sec
- Reads: ~10B/month ≈ ~4000/sec (peak ~10K/sec)
- Storage per URL: ~500 bytes → 100M × 500B × 12 months × 5 years ≈ ~3 TB over 5 years

---

## 2. API Design

```
POST /api/shorten
  Body: { "long_url": "https://example.com/very/long/path", "ttl_days": 365 }
  Response: { "short_url": "https://sho.rt/aB3xK9z", "expires_at": "2027-03-07" }

GET /{short_code}
  → 301 Redirect to long_url
  (301 for SEO-friendly permanent redirect; use 302 if analytics accuracy is critical)

GET /api/stats/{short_code}
  Response: { "long_url": "...", "clicks": 142857, "created_at": "...", "expires_at": "..." }

DELETE /api/{short_code}
  → 204 No Content
```

---

## 3. High-Level Architecture

```
                         ┌─────────────┐
                         │   CDN/Edge   │  (optional, cache hot redirects)
                         └──────┬──────┘
                                │
                                ▼
┌────────┐            ┌─────────────────┐            ┌──────────────┐
│ Client │───────────▶│   API Gateway    │───────────▶│  App Server  │
│        │            │  (Rate Limiter)  │            │  (Stateless) │
└────────┘            └─────────────────┘            └──────┬───────┘
                                                            │
                                              ┌─────────────┼─────────────┐
                                              ▼             ▼             ▼
                                       ┌───────────┐ ┌───────────┐ ┌───────────┐
                                       │   Cache    │ │  Database  │ │  Counter  │
                                       │  (Redis)   │ │ (Postgres) │ │  (Redis)  │
                                       └───────────┘ └───────────┘ └───────────┘
```

**Single service, not microservices.** One app server handles both shorten + redirect.

---

## 4. Data Model

### `urls` table (PostgreSQL)

```sql
CREATE TABLE urls (
    short_code   VARCHAR(7) PRIMARY KEY,
    long_url     TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMP,
    user_id      BIGINT,              -- optional, for authenticated users
    click_count  BIGINT DEFAULT 0
);

CREATE INDEX idx_urls_expires ON urls(expires_at) WHERE expires_at IS NOT NULL;
```

---

## 5. Short Code Generation Strategy

### Approach: Base62-encoded Counter (Recommended)

```
Counter (auto-increment or distributed ID) → Base62 encode → 7-char code
```

**How it works:**
1. Use a PostgreSQL `SEQUENCE` or a Redis `INCR` counter
2. Convert the integer to base62 (a-z, A-Z, 0-9)
3. Pad to 7 characters

```
counter = 1000000  → base62 → "4c92"  → pad → "0004c92"
counter = 1000001  → base62 → "4c93"  → pad → "0004c93"
```

**Pros:** Zero collisions, simple, sequential  
**Cons:** Predictable (solvable by XOR with a secret or use a bijective shuffle)

### Alternative: Random + Check

```
1. Generate random 7-char base62 string
2. Check DB for collision
3. If collision, regenerate (astronomically rare with 3.5T space)
```

**Anti-predictability trick:** Use a simple Feistel cipher or XOR-shuffle on the counter:
```
real_id = 1000000
shuffled = feistel_encrypt(real_id, secret_key)  → e.g., 2847193
short_code = base62(2847193) → "Bx3kP"
```
This gives non-sequential, non-guessable codes with zero collisions.

---

## 6. Core Workflows

### 6a. Create Short URL

```
Client                    App Server                 Redis              Postgres
  │                           │                        │                    │
  │── POST /api/shorten ─────▶│                        │                    │
  │                           │── INCR url_counter ───▶│                    │
  │                           │◀── 1000042 ────────────│                    │
  │                           │                        │                    │
  │                           │  base62(shuffle(1000042)) = "kR4mBz"       │
  │                           │                        │                    │
  │                           │── INSERT (kR4mBz, url) ───────────────────▶│
  │                           │◀── OK ─────────────────────────────────────│
  │                           │                        │                    │
  │                           │── SET kR4mBz → url ───▶│  (warm cache)     │
  │                           │                        │                    │
  │◀── { short_url } ────────│                        │                    │
```

### 6b. Redirect (Read path — hot path)

```
Client                    App Server                 Redis              Postgres
  │                           │                        │                    │
  │── GET /kR4mBz ──────────▶│                        │                    │
  │                           │── GET kR4mBz ─────────▶│                    │
  │                           │◀── "https://long..." ──│  ✓ cache hit       │
  │                           │                        │                    │
  │                           │── INCR clicks:kR4mBz ─▶│  (async counter)  │
  │                           │                        │                    │
  │◀── 301 Redirect ─────────│                        │                    │
```

On cache miss:
```
  │                           │── GET kR4mBz ─────────▶│                    │
  │                           │◀── null ───────────────│  ✗ cache miss      │
  │                           │                        │                    │
  │                           │── SELECT long_url ... ────────────────────▶│
  │                           │◀── "https://long..." ──────────────────────│
  │                           │                        │                    │
  │                           │── SET kR4mBz → url ───▶│  (populate cache)  │
  │                           │                        │                    │
  │◀── 301 Redirect ─────────│                        │                    │
```

---

## 7. Caching Strategy

- **Redis** as look-aside cache for `short_code → long_url` mapping
- **TTL on cache entries**: match URL expiration, or 24h for popular links
- **Cache hit ratio target**: 90%+ (Zipf distribution — a small % of URLs get most traffic)
- **Eviction**: LRU, Redis handles this natively with `maxmemory-policy allkeys-lru`

**Sizing:** 10M hot URLs × ~300 bytes = ~3 GB → fits in a single Redis instance

---

## 8. Analytics (Click Counting)

Don't block the redirect on a DB write. Two-tier approach:

1. **Real-time:** `INCR clicks:{short_code}` in Redis (O(1), non-blocking)
2. **Batch flush:** A background job every 60 seconds:
   ```sql
   UPDATE urls SET click_count = click_count + <delta>
   WHERE short_code = <code>;
   ```
   Then reset the Redis counter.

For richer analytics (timestamp, geo, referrer), push events to a log/queue and process async — but this is beyond the 45-min scope.

---

## 9. Rate Limiting & Abuse Prevention

**Rate limiting (at API Gateway):**
- Create: 10 req/min per IP (unauthenticated), 100 req/min per user
- Redirect: 1000 req/min per IP (generous, but catches bots)
- Implementation: Redis sliding window counter per `(IP, endpoint)`

**Abuse prevention:**
- Block known malicious URLs via blocklist check (e.g., Google Safe Browsing API)
- Don't allow shortening of already-short URLs (no redirect chains)
- CAPTCHA after threshold for anonymous creation
- Flag URLs with abnormally high click rates for review

---

## 10. Expiration & Cleanup

- A cron job / scheduled task runs daily:
  ```sql
  DELETE FROM urls WHERE expires_at < NOW();
  ```
- Also evicts the corresponding Redis cache keys
- For soft-delete: add a `deleted_at` column, purge after 30 days

---

## 11. Scaling Considerations

| Component | Strategy |
|-----------|----------|
| App Server | Stateless → horizontal scale behind load balancer |
| Database | Read replicas for redirect reads; single primary for writes (40/sec is trivial) |
| Cache | Redis Cluster if >1 node needed; but 3 GB fits one instance |
| Counter | Single Redis INCR (atomic); or pre-allocate ranges per app server if needed |
| Storage growth | Partition `urls` table by `created_at`; archive/drop old partitions |

**At extreme scale (10x+):**
- Put redirect behind a CDN edge (cache 301s at edge PoPs)
- Shard Postgres by `short_code` prefix (a-m on shard1, n-z on shard2)
- Pre-allocate counter ranges: each server gets a block of 10K IDs at a time → eliminates Redis counter as bottleneck

---

## 12. Security

- **Input validation:** Validate `long_url` is a valid URL with allowed schemes (http/https only)
- **No open redirect abuse:** Log and monitor; rate-limit creation
- **HTTPS everywhere**
- **Short codes are not secret:** Don't use them to gate access to sensitive content

---

## Summary

| Concern | Solution |
|---------|----------|
| Code generation | Counter + base62 + Feistel shuffle (collision-free, non-guessable) |
| Storage | PostgreSQL (durable, relational, simple) |
| Caching | Redis look-aside cache, LRU eviction |
| Read latency | Cache hit → <5ms; cache miss → <20ms |
| Analytics | Redis INCR + batch flush to DB |
| Rate limiting | Sliding window in Redis at API gateway |
| Expiration | TTL on rows + daily cleanup job |
| Scaling | Stateless servers, read replicas, CDN for redirects |

**Key trade-off:** Using a counter + shuffle gives us the best of both worlds — zero collisions (unlike random) and non-guessable codes (unlike plain sequential). The entire system is a single service backed by Postgres + Redis, no need for Kafka or microservices at this scale.

