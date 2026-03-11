# Design a Product Search / Catalog System
**Full-text search across millions of products with faceted filtering — Shopify interview (45 min)**

---

## 1. Requirements (~3 min)

### Functional Requirements
1. Merchants manage products (create, update, delete) — stored in Postgres
2. Customers search by free text (title, description) across a merchant's catalog
3. **Faceted filtering** — filter by category, price range, tags, in-stock status; return facet counts
4. Near real-time indexing — product changes searchable within seconds

### Non-Functional Requirements
1. **~10M products** across all merchants, ~1K search req/sec avg, spikes to ~10K/sec
2. Search latency **< 200ms** P99 (read-heavy, cacheable)
3. **Multi-tenant isolation** — a customer never sees another merchant's products
4. Write throughput: ~100 product updates/sec (trivially low)
5. **20 GB** index fits in memory (10M × ~2KB per document)
6. Read:Write ratio ~100:1

---

## 2. Core Entities (~2 min)

- **Product** — the item being sold (source of truth in Postgres)
- **IndexQueue** — tracks dirty products that need syncing to Elasticsearch
- **SearchIndex** — Elasticsearch documents (read-optimized projection of products)

---

## 3. API (~3 min)

```
# Customer-facing search
GET  /v1/search?shop_id=123&q=blue+t-shirt&category=clothing
                &min_price=10&max_price=50&in_stock=true&page=1&limit=20
→ {
    "results": [ { product_id, title, price, image_url, tags, in_stock } ],
    "total": 843,
    "facets": {
      "category": [{ "value": "clothing", "count": 400 }, ...],
      "tags":     [{ "value": "sale", "count": 120 }, ...]
    }
  }

# Merchant catalog management (write side)
POST   /v1/products                    → create product (triggers index)
PUT    /v1/products/:product_id        → update product (triggers re-index)
DELETE /v1/products/:product_id        → remove product (triggers index delete)
GET    /v1/products/:product_id        → get single product (from DB, not search)
```

---

## 4. High-Level Architecture

**Single monolithic app — no microservices.** One app server handles search reads and catalog writes. Background workers poll the index queue to sync Postgres → Elasticsearch.

```
┌──────────────────────────────────────────────────┐
│                 Client Layer                      │
│                                                  │
│   ┌──────────────┐       ┌───────────────────┐   │
│   │ Customer      │       │  Merchant Portal  │   │
│   │ (searches)    │       │  (catalog CRUD)   │   │
│   └──────┬───────┘       └───────┬───────────┘   │
└──────────┼───────────────────────┼───────────────┘
           │                       │
           ▼                       ▼
┌──────────────────────────────────────────────────┐
│           Service Application Layer               │
│                                                  │
│  ┌────────────────┐  ┌───────────────────────┐   │
│  │  Search API    │  │  Catalog API          │   │
│  │  (GET /search) │  │  (CRUD /products)     │   │
│  └────────────────┘  └───────────────────────┘   │
│                                                  │
│  Callout:                                        │
│  Product writes go to Postgres first, then       │
│  enqueue to index_queue in SAME transaction.     │
│  Search reads go to Elasticsearch directly.      │
│                                                  │
│  Requirements:                                   │
│  * Postgres is source of truth                   │
│  * ES is a read-optimized projection             │
│  * shop_id always injected server-side (tenant   │
│    isolation)                                    │
└──────────────────┬───────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────┐
│                  Data Layer                       │
│                                                  │
│  ┌──────────────────────┐  ┌─────────────────┐   │
│  │     Postgres DB       │  │ Elasticsearch   │   │
│  │                       │  │                 │   │
│  │  * products table     │  │ * search index  │   │
│  │  * index_queue table  │  │ * facets/aggs   │   │
│  │    (state-driven)     │  │                 │   │
│  └──────────────────────┘  └─────────────────┘   │
│                                                  │
│  ┌──────────────────────┐                        │
│  │     Redis Cache       │                        │
│  │                       │                        │
│  │  * search result      │                        │
│  │    cache (60s TTL)    │                        │
│  └──────────────────────┘                        │
└──────────────────┬───────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────┐
│              Async System Layer                   │
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │ Indexing Worker                            │  │
│  │                                            │  │
│  │ * poll index_queue WHERE                   │  │
│  │   processed_at IS NULL                     │  │
│  │ * FOR UPDATE SKIP LOCKED                   │  │
│  │ * fetch full product row from Postgres     │  │
│  │ * PUT document to Elasticsearch            │  │
│  │ * mark processed_at = NOW()                │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │ Sweeper Worker                             │  │
│  │                                            │  │
│  │ * find stuck index_queue rows              │  │
│  │   (claimed but not processed > 5 min)      │  │
│  │ * reset processed_at = NULL                │  │
│  │ * prune old processed rows from queue      │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │ Rebuild Worker (on-demand)                 │  │
│  │                                            │  │
│  │ * full reindex from Postgres → ES          │  │
│  │ * bulk API, batches of 1000                │  │
│  │ * zero-downtime via ES index aliases       │  │
│  └────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

---

## 5. Table Schemas

```
┌──────────────────────────────────────────┐
│ table : products                         │
│ (source of truth)                        │
│                                          │
│ product_id (PK)                          │
│ shop_id                                  │
│ title                                    │
│ description                              │
│ price                                    │
│ compare_price                            │
│ category                                 │
│ tags (TEXT[])                             │
│ in_stock (boolean, default true)         │
│ inventory_count (default 0)              │
│ image_url                                │
│ status (active/archived/draft)           │
│ created_at                               │
│ updated_at                               │
│                                          │
│ INDEX (shop_id, status)                  │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│ table : index_queue                      │
│ (the sync queue — workers poll this)     │
│                                          │
│ queue_id (PK)                            │
│ product_id (FK)                          │
│ operation (index/delete)                 │
│ created_at                               │
│ claimed_at (nullable)                    │
│ processed_at (nullable)                  │
│                                          │
│ INDEX (processed_at, created_at)         │
│ — workers poll WHERE processed_at IS NULL│
└──────────────────────────────────────────┘
```

### Elasticsearch Document

```json
{
  "product_id": 456,
  "shop_id": 123,
  "title": "Blue Cotton T-Shirt",
  "description": "Soft, breathable cotton...",
  "price": 29.99,
  "category": "clothing",
  "tags": ["sale", "summer", "cotton"],
  "in_stock": true,
  "status": "active",
  "created_at": "2026-01-01T00:00:00Z"
}
```

**ES index mapping:** `title` and `description` as `text` (analyzed, BM25 scoring). All filter fields (`price`, `category`, `tags`, `in_stock`, `shop_id`) as `keyword`/`numeric` for fast filtering + faceting. Routing key = `shop_id`.

Callout: **the index_queue table IS the sync mechanism.** No Kafka or Redis Streams needed. Workers poll `processed_at IS NULL` directly with `FOR UPDATE SKIP LOCKED` — the same pattern as the webhook deliveries table.

---

## 6. Key Flows

### Flow 1: Sync — Merchant Updates Product (low latency, same transaction)

```
Merchant updates product in portal
    │
    ▼
Catalog API handler:
    1. Auth: verify merchant owns product (shop_id from auth token)
    2. In ONE transaction:
       UPDATE products SET title=..., price=... WHERE product_id=456
       INSERT INTO index_queue (product_id, operation) VALUES (456, 'index')
    3. Return 200 to merchant immediately
```

Callout: same-transaction insert into index_queue guarantees **no orphaned products** — even if the indexing worker is down, dirty products persist in the queue and sync when it recovers.

### Flow 2: Sync — Customer Search Request

```
GET /v1/search?shop_id=123&q=blue+t-shirt&category=clothing&min_price=10

App Server:
    │
    ▼
    1. Check Redis cache: GET search:{sha256(shop_id + query_params)}
       → Cache hit: return cached response immediately
       → Cache miss: continue to step 2
    │
    ▼
    2. Build ES query:
       {
         "query": {
           "bool": {
             "must": {
               "multi_match": {
                 "query": "blue t-shirt",
                 "fields": ["title^3", "description"]
               }
             },
             "filter": [
               { "term":  { "shop_id": 123 } },
               { "term":  { "status": "active" } },
               { "term":  { "category": "clothing" } },
               { "range": { "price": { "gte": 10 } } }
             ]
           }
         },
         "aggs": {
           "by_category": { "terms": { "field": "category" } },
           "by_tags":     { "terms": { "field": "tags" } },
           "price_stats": { "stats": { "field": "price" } }
         }
       }
    │
    ▼
    3. ES returns hits + aggregations (facet counts)
    4. Cache result in Redis (TTL 60s)
    5. Return results + facets to customer
```

**Key:** `shop_id` is ALWAYS in the `filter` clause, injected server-side from auth context — this is both a correctness requirement (tenant isolation) and a performance optimization (ES routing key skips other shards).

### Flow 3: Async — Indexing Worker

```
Indexing Worker (polls every 1-2 seconds):
    │
    ▼
    1. Claim ready queue items:
       UPDATE index_queue
       SET claimed_at = NOW()
       WHERE queue_id IN (
           SELECT queue_id FROM index_queue
           WHERE processed_at IS NULL AND claimed_at IS NULL
           ORDER BY created_at
           LIMIT 100
           FOR UPDATE SKIP LOCKED
       )
       RETURNING *;
    │
    ▼
    2. For each claimed item:
       ├── operation = 'index':
       │   Fetch full product row from Postgres
       │   PUT to ES: /products/_doc/{product_id} (with routing=shop_id)
       │
       └── operation = 'delete':
           DELETE from ES: /products/_doc/{product_id} (with routing=shop_id)
    │
    ▼
    3. Mark processed: UPDATE index_queue SET processed_at = NOW()
       WHERE queue_id IN (...)
```

### Flow 4: Async — Sweeper Worker

```
Every 60 seconds:
    1. Reclaim stuck items:
       UPDATE index_queue SET claimed_at = NULL
       WHERE claimed_at IS NOT NULL
         AND processed_at IS NULL
         AND claimed_at < NOW() - INTERVAL '5 minutes';

    2. Prune old processed items:
       DELETE FROM index_queue
       WHERE processed_at IS NOT NULL
         AND processed_at < NOW() - INTERVAL '1 day';
```

Callout: if indexing worker crashes between claiming and processing — sweeper resets the row. **No product update is ever lost from the index.**

---

## 7. Index Queue State Machine

```
index_queue row lifecycle:

  UNCLAIMED ──────▶ CLAIMED ──────▶ PROCESSED  (terminal, pruned after 1 day)
  (claimed_at       (claimed_at     (processed_at
   = NULL)           = NOW())        = NOW())
     ▲                  │
     │                  │ (worker crash / timeout > 5 min)
     │                  │
     └── sweeper ───────┘  resets claimed_at = NULL
```

---

## 8. Deep Dives

### Deep Dive 1: Faceted Filtering

ES `aggs` (aggregations) return facet counts in the same query as search results — no extra round trip.

- Filters in `filter` context are cached by ES and don't affect relevance scoring → fast
- Only `multi_match` on text fields goes in `query` context (affects BM25 score)
- Facet counts are computed efficiently: ES scores matching docs but counts all filtered docs per bucket

### Deep Dive 2: Relevance Ranking

Default BM25 scoring works well. Boost title matches over description:
```json
"fields": ["title^3", "description^1"]
```

For Shopify: also boost by `in_stock: true` using `function_score` with a filter boost, and add a small recency decay on `created_at`. For the interview, BM25 + title boost is sufficient.

### Deep Dive 3: Multi-Tenancy Isolation

Every ES query **must** include `"term": { "shop_id": 123 }` in the filter. Enforced server-side, never from client. Options:

- **Shared index + shop_id filter (current design):** Simple, cost-efficient. Works to ~100M products.
- **Index-per-shop:** Better isolation, but 1M+ merchants = 1M+ indices — not viable in ES.
- **Shard routing:** Use `shop_id` as routing key → same-shop docs co-locate on one shard → faster queries.

For the interview: shared index + `shop_id` filter + routing key.

### Deep Dive 4: What if Elasticsearch is Down?

- **Writes:** Index queue in Postgres ensures no data loss — products buffered and synced when ES recovers.
- **Reads:** Fall back to Postgres full-text search (`tsvector` / `websearch_to_tsquery`). Slower and no facets, but the storefront stays up. Return simplified results without facet counts.

### Deep Dive 5: Scaling

**Why this architecture works at Shopify scale:**
- `FOR UPDATE SKIP LOCKED` turns the index_queue into a high-performance job queue
- ES handles 10K search req/sec easily with 3 primary shards + replicas
- Redis cache absorbs hot queries (>90% cache hit rate on Zipf-distributed traffic)
- Partition index_queue by `created_at` (weekly), drop old partitions

**When to add Kafka:**
- Only if index_queue polling latency becomes an issue → add Kafka as a "wake up" signal
- Postgres remains source of truth; Kafka just notifies workers faster
- Only add when polling contention becomes a measurable problem

### Deep Dive 6: Security

- **Tenant isolation:** `shop_id` always injected server-side from auth context, never from query params
- **Input sanitization:** Use `multi_match` with explicit fields — never raw `query_string` which allows ES query injection
- **Rate limiting:** Per-shop search rate limit (100 req/sec per shop) to prevent scraping
- **Field projection:** Never expose `shop_id`, `inventory_count` to customers in search response

---

## 9. Summary

| Concern | Solution |
|---------|----------|
| Full-text search | Elasticsearch `multi_match` + BM25 scoring |
| Faceted filters | ES aggregations in same query (single round trip) |
| Write durability | Postgres is source of truth; same-txn insert into index_queue |
| No separate queue | Workers poll `index_queue.processed_at` directly |
| Near-real-time sync | Background worker with `FOR UPDATE SKIP LOCKED` |
| Stuck recovery | Sweeper resets stale `claimed_at` → unclaimed |
| Tenant isolation | `shop_id` filter on every ES query, enforced server-side |
| Caching | Redis 60s TTL for search responses |
| ES down | Fallback to Postgres `tsvector` search |
| Scale path | More workers → Kafka notification → partition/shard |

Callout: The main design choice is **Postgres as source of truth, ES as a read projection.** The index_queue table is the sync mechanism — no Kafka, no Redis Streams. Workers poll `processed_at IS NULL` directly. This mirrors the pattern used in the Webhook Delivery design where workers poll `deliveries.status` — simple, durable, no data loss.
