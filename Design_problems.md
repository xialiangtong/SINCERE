# System Design Interview Questions — Shopify

## Tier 1: Most Frequently Reported

| # | Problem | Why Shopify Cares |
|---|---|---|
| 1 | **Design a Flash Sale / High-Traffic Sale System** | Core business — handling 10K+ orders/sec during sales events, inventory consistency |
| 2 | **Design a Checkout / Cart System** | Core e-commerce flow — idempotent payments, inventory reservation, cart expiry |
| 3 | **Design an Inventory Management System** | Preventing overselling across multiple sales channels (online, POS, wholesale) |
| 4 | **Design a Webhook Delivery System** | Shopify delivers billions of webhooks to merchants — reliable, at-least-once delivery |

## Tier 2: Commonly Asked

| # | Problem | Why Shopify Cares |
|---|---|---|
| 5 | **Design a URL Shortener** (simpler, for mid-level) | Classic warm-up, tests DB design + caching + high read throughput |
| 6 | **Design a Rate Limiter** | Shopify's API is heavily rate-limited per merchant — sliding window, token bucket |
| 7 | **Design a Notification System** | Merchant notifications across email/SMS/push — very similar to what you've already designed |
| 8 | **Design a Product Search / Catalog System** | Full-text search across millions of products, faceted filtering |

## Tier 3: Occasionally Reported

| # | Problem | Why Shopify Cares |
|---|---|---|
| 9 | **Design a Multi-Tenant SaaS Platform** | Shopify itself — tenant isolation, data partitioning, per-tenant rate limiting |
| 10 | **Design a Payment Processing System** | Idempotent charges, refund handling, PCI compliance |
| 11 | **Design a CDN / Asset Delivery System** | Serving storefront assets globally with low latency |
| 12 | **Design a Job Queue / Background Processing System** | Shopify uses Sidekiq/Redis heavily for async work — very relevant to your worker designs |

## Overlap with Existing Prep

| Your Existing Design | Shopify Question It Maps To |
|---|---|
| **Library Book Return** (3-table, workers polling state) | Job Queue / Background Processing, Webhook Delivery |
| **Merchant Photo** (3-table, assign/quality/notify workers) | Notification System, Multi-Tenant SaaS |
| **RateLimiter solution** (Java) | Rate Limiter |
| **ShortURL solution** (Java) | URL Shortener |

### Gaps to Fill
- Flash Sale / Checkout
- Inventory Management
- Product Search
