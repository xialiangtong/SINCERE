# Design: Checkout / Cart System (40 min)

## 1. Requirements (~5 min)

### Functional Requirements
1. Customer adds/removes products to a **shopping cart**
2. Customer proceeds to **checkout** (cart → order)
3. System **reserves inventory** when checkout begins (prevents overselling)
4. Customer completes **payment** (integrates with payment gateway)
5. Cart **expires** if abandoned (releases reserved inventory)

### Non-Functional Requirements
1. **Strong consistency** on inventory — no overselling
2. **Idempotent checkout** — double-click / network retry must not double-charge
3. **Low latency** — cart operations < 100ms, checkout < 2s
4. **High availability** — cart must survive server restarts (persistent, not session-only)
5. **Scale** — support 100K+ concurrent carts, 1K checkouts/sec

---

## 2. Core Entities (~2 min)

- **Cart** — a user's collection of items, persisted
- **CartItem** — product + quantity in a cart
- **InventoryReservation** — temporary hold during checkout (TTL-based)
- **Order** — finalized purchase
- **OrderItem** — line item in an order

---

## 3. API (~5 min)

```
# Cart API (authenticated via user token)
POST   /v1/cart/items              → add item { product_id, quantity }
PUT    /v1/cart/items/:productId   → update quantity { quantity }
DELETE /v1/cart/items/:productId   → remove item
GET    /v1/cart                    → get full cart (items + prices + availability)
DELETE /v1/cart                    → clear cart

# Checkout API
POST   /v1/checkout                → initiate checkout (reserve inventory, create pending order)
       Headers: Idempotency-Key: <UUID>
       Body: { shipping_address, payment_method_token }

POST   /v1/checkout/:orderId/pay   → confirm payment
       Headers: Idempotency-Key: <UUID>

GET    /v1/orders/:orderId         → get order status
```

---

## 4. Data Schema

### Option A: DB-backed cart (Shopify style — persistent, cross-device)

```
┌───────────────────────────────────┐
│ table: carts                      │
│                                   │
│ cart_id (PK)                      │
│ user_id (FK, UNIQUE)              │
│ status (active/checkout/abandoned)│
│ last_active_at                    │
│ created_at                        │
│ updated_at                        │
└───────────────────────────────────┘

┌───────────────────────────────────┐
│ table: cart_items                  │
│                                   │
│ cart_item_id (PK)                 │
│ cart_id (FK)                      │
│ product_id (FK)                   │
│ variant_id (FK)                   │
│ quantity                          │
│ unit_price (snapshot at add time) │
│ created_at                        │
│ updated_at                        │
│                                   │
│ UNIQUE(cart_id, variant_id)       │
└───────────────────────────────────┘

┌───────────────────────────────────────┐
│ table: products                       │
│                                       │
│ product_id (PK)                       │
│ name                                  │
│ description                           │
│ merchant_id (FK)                      │
│ created_at                            │
└───────────────────────────────────────┘

┌───────────────────────────────────────┐
│ table: product_variants               │
│                                       │
│ variant_id (PK)                       │
│ product_id (FK)                       │
│ sku                                   │
│ price                                 │
│ inventory_count                       │
│ reserved_count                        │
│ (available = inventory_count          │
│            - reserved_count)          │
│ updated_at                            │
└───────────────────────────────────────┘

┌───────────────────────────────────────┐
│ table: orders                         │
│                                       │
│ order_id (PK)                         │
│ idempotency_key (UNIQUE)              │
│ user_id (FK)                          │
│ status                                │
│ (pending/payment_processing/          │
│  completed/failed/cancelled/refunded) │
│ shipping_address (json)               │
│ subtotal                              │
│ tax                                   │
│ total                                 │
│ paid_at                               │
│ created_at                            │
│ updated_at                            │
└───────────────────────────────────────┘

┌───────────────────────────────────────┐
│ table: order_items                    │
│                                       │
│ order_item_id (PK)                    │
│ order_id (FK)                         │
│ variant_id (FK)                       │
│ quantity                              │
│ unit_price (locked at checkout time)  │
│ created_at                            │
└───────────────────────────────────────┘

┌───────────────────────────────────────┐
│ table: inventory_reservations         │
│                                       │
│ reservation_id (PK)                   │
│ order_id (FK)                         │
│ variant_id (FK)                       │
│ quantity                              │
│ status (reserved/committed/released)  │
│ expires_at (e.g. now + 15 min)        │
│ created_at                            │
└───────────────────────────────────────┘
```

---

## 5. High-Level Architecture

```
                    ┌──────────┐
                    │   CDN    │ ← product images, static assets
                    └────┬─────┘
                         │
  ┌──────────────────────▼──────────────────────────┐
  │               API Gateway                        │
  │  (Auth, Rate Limit, Idempotency-Key extraction)  │
  └──┬──────────────────┬──────────────────┬─────────┘
     │                  │                  │
     ▼                  ▼                  ▼
┌──────────┐    ┌──────────────┐    ┌────────────┐
│  Cart    │    │  Checkout    │    │  Order     │
│  Service │    │  Service     │    │  Query     │
│          │    │              │    │  Service   │
└────┬─────┘    └──────┬───────┘    └─────┬──────┘
     │                 │                  │
     ▼                 ▼                  │
┌──────────┐    ┌──────────────┐          │
│  Redis   │    │  Inventory   │          │
│  (cart   │    │  Service     │          │
│  cache)  │    │              │          │
└──────────┘    └──────┬───────┘          │
     │                 │                  │
     └─────────────────┼──────────────────┘
                       ▼
                ┌──────────────┐
                │  PostgreSQL  │
                │  (source of  │
                │   truth)     │
                └──────────────┘
                       │
                       ▼
                ┌──────────────┐
                │  Payment     │
                │  Gateway     │
                │ (Stripe/etc) │
                └──────────────┘

Workers:
  ┌─────────────────────────┐  ┌──────────────────────────┐
  │ Reservation Expiry      │  │ Cart Cleanup             │
  │ Worker                  │  │ Worker                   │
  │ • release expired       │  │ • mark carts as          │
  │   reservations          │  │   abandoned after 30 days│
  │ • decrement             │  │ • optionally send        │
  │   reserved_count        │  │   "abandoned cart" email  │
  └─────────────────────────┘  └──────────────────────────┘
```

---

## 6. Key Flows

### Flow 1: Add to Cart

```
POST /v1/cart/items { product_id: "P1", quantity: 2 }
    │
    ▼
Cart Service:
    1. Check product exists + is active
    2. Check available inventory (inventory_count - reserved_count >= quantity)
       → Note: this is a SOFT check — not reserved yet, just availability signal
    3. UPSERT into cart_items (cart_id, variant_id, quantity)
    4. Invalidate Redis cart cache
    5. Return updated cart
```

**No inventory reservation here** — we only reserve at checkout. Otherwise users holding items in cart would block others from buying.

### Flow 2: Checkout (the critical flow)

```
POST /v1/checkout
Headers: Idempotency-Key: "abc-123"
Body: { shipping_address: {...}, payment_method_token: "tok_xxx" }
    │
    ▼
Checkout Service:
    1. Check idempotency_key → if order exists, return existing order (no double-create)
    
    2. BEGIN TRANSACTION
       a. Lock cart: UPDATE carts SET status='checkout' WHERE user_id=? AND status='active'
          → if 0 rows: cart already in checkout or empty
       
       b. For each cart item, reserve inventory:
          UPDATE product_variants
          SET reserved_count = reserved_count + cart_item.quantity
          WHERE variant_id = cart_item.variant_id
            AND (inventory_count - reserved_count) >= cart_item.quantity
          → if 0 rows: insufficient inventory, ROLLBACK, return "Item X is out of stock"
       
       c. Create order (status='pending') + order_items (price locked at current price)
       
       d. Create inventory_reservations (status='reserved', expires_at=now+15min)
       
       e. Clear cart_items, reset cart status='active'
    COMMIT
    
    3. Return order (status='pending', order_id)
```

**Why one transaction?** Reserve inventory + create order must be atomic. If we reserve but crash before creating the order, the reservation expiry worker will clean up.

### Flow 3: Pay (confirm payment)

```
POST /v1/checkout/:orderId/pay
Headers: Idempotency-Key: "def-456"
    │
    ▼
Checkout Service:
    1. Check idempotency — if already paid, return existing result
    2. Validate order.status == 'pending'
    3. UPDATE orders SET status='payment_processing'
    
    4. Call Payment Gateway (Stripe):
       stripe.charges.create({
         amount: order.total,
         currency: 'usd',
         source: payment_method_token,
         idempotency_key: "def-456"  ← Stripe also supports idempotency
       })
    
    5a. On SUCCESS:
        UPDATE orders SET status='completed', paid_at=now()
        UPDATE inventory_reservations SET status='committed'
        UPDATE product_variants SET inventory_count = inventory_count - quantity
        (actual deduction happens here — reserved_count stays until committed)
    
    5b. On FAILURE:
        UPDATE orders SET status='failed'
        Release reservations:
          UPDATE inventory_reservations SET status='released'
          UPDATE product_variants SET reserved_count = reserved_count - quantity
```

### Flow 4: Reservation Expiry Worker

```
Every 30 seconds:
    SELECT * FROM inventory_reservations
    WHERE status = 'reserved' AND expires_at < now()
    FOR UPDATE SKIP LOCKED;

    For each:
      - SET status = 'released'
      - UPDATE product_variants SET reserved_count = reserved_count - quantity
      - UPDATE orders SET status = 'failed' WHERE order_id = reservation.order_id
```

This catches abandoned checkouts — user started checkout but never paid.

---

## 7. Deep Dives (~10 min)

### Deep Dive 1: Cart Storage — Redis vs DB vs Hybrid

| Approach | Pros | Cons |
|---|---|---|
| **Redis only** | Fastest reads/writes | Lost on Redis crash, no cross-device |
| **DB only** | Persistent, queryable, cross-device | Slower for frequent updates |
| **Hybrid (recommended)** | Redis as read cache, DB as source of truth | Slightly more complex |

**Recommended:** Write to DB, cache full cart in Redis. Most cart reads hit cache. Cache invalidation on any cart mutation.

```
Redis key: cart:{user_id}
TTL: 1 hour (auto-refresh on read)
Value: serialized cart JSON (items + prices + availability)
```

### Deep Dive 2: Price Consistency

**Problem:** Customer adds item at $10, price changes to $15 before checkout. What price do they pay?

**Shopify's approach:**
- Cart shows **live prices** (recalculated on every `GET /cart`)
- `cart_items.unit_price` is a **snapshot** for display, refreshed on cart load
- At checkout, price is **locked** into `order_items.unit_price`
- If price changed significantly, show a warning: "Price has changed since you added this item"

### Deep Dive 3: Inventory Consistency Across Channels

**Problem:** Shopify merchants sell on web, mobile, POS, Amazon, Facebook — all sharing the same inventory.

**Solution:** Single `product_variants.inventory_count` as source of truth:
- All channels go through the same Inventory Service
- `reserved_count` tracks holds across all channels
- `available = inventory_count - reserved_count`

For extreme scale, use Redis as a fast availability cache:
```
Redis: variant:{id}:available → atomic integer
On checkout: DECR in Redis first (fast gate), then confirm in Postgres
```

Same pattern as the Flash Sale design — Redis gatekeeper, Postgres source of truth.

### Deep Dive 4: Abandoned Cart Recovery

```
Cart Cleanup Worker (runs hourly):
    SELECT c.* FROM carts c
    WHERE c.status = 'active'
      AND c.last_active_at < now() - interval '24 hours'
      AND EXISTS (SELECT 1 FROM cart_items ci WHERE ci.cart_id = c.cart_id);

    For each abandoned cart with items:
      - Send "You left items in your cart" email
      - After 30 days with no activity:
        UPDATE carts SET status='abandoned'
```

This is a **revenue recovery feature** — abandoned cart emails recover 5-10% of lost sales. The notification uses the same `notification_state` pattern from your other designs, or can be a simple email job.

---

## 8. State Machines

### Cart States
```
active → checkout → active (if checkout fails / cancel)
active → abandoned (after 30 days inactive)
```

### Order States
```
pending → payment_processing → completed
pending → payment_processing → failed
pending → failed (reservation expired before payment)
completed → refunded (merchant initiates refund)
pending → cancelled (user cancels before paying)
```

### Reservation States
```
reserved → committed (payment succeeded)
reserved → released (payment failed or reservation expired)
```

---

## 9. Trade-offs / Cons

| Trade-off | Consequence |
|---|---|
| No reservation at add-to-cart time | Item might go out of stock between cart and checkout — user sees error at checkout |
| 15-min reservation TTL | Too short: user can't finish. Too long: blocks other buyers |
| DB-backed cart (not session) | Heavier writes, but enables cross-device + abandoned cart recovery |
| Synchronous payment | User waits 1-3s for Stripe response. Can go async for extreme scale |
| Single inventory_count column | All channels share one counter — simpler but potential bottleneck at very high scale |

---

## 10. Presenter Notes (40-min pacing)

| Section | Time | Focus |
|---|---|---|
| Requirements | 5 min | Emphasize: no overselling, idempotent payment, cart expiry |
| Entities + API | 5 min | Show idempotency_key in checkout + pay endpoints |
| Schema | 5 min | 6 tables — highlight `reserved_count` on variants and `inventory_reservations` |
| Architecture | 5 min | Draw diagram, highlight Cart Service + Checkout Service separation |
| Checkout Flow | 8 min | The star — single transaction: reserve + create order atomically |
| Deep Dive: Price consistency | 3 min | Snapshot at add, lock at checkout |
| Deep Dive: Multi-channel inventory | 3 min | Single source of truth + Redis gate for scale |
| Deep Dive: Abandoned cart | 3 min | Revenue recovery, email notification |
| Trade-offs | 3 min | No reservation at add-to-cart, TTL tuning |

---

## 11. How This Differs from Flash Sale Design

| Aspect | Flash Sale | Normal Checkout |
|---|---|---|
| Traffic pattern | Massive spike at sale start | Steady throughout day |
| Queue gate | Yes (virtual queue) | No (not needed) |
| Inventory reserve timing | At "Buy Now" click (immediate) | At checkout initiation |
| Cart | Usually skip cart (direct buy) | Full cart with browsing |
| TTL for reservation | Short (10 min) | Longer (15 min) |
| Redis dependency | Critical (gatekeeper) | Optional (cache/optimization) |
