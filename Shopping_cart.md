Problem statement

A merchant on Shopify sells products identified by a product code (SKU). Build a small “shopping cart” library that supports:

Core cart operations

addItem(code, quantity)

removeItem(code, quantity) (or setQuantity(code, quantity))

getItems() (optional)

subtotal() – total before discounts

totalDiscount() – total discount applied

total() – final amount after discounts (and optionally shipping/tax)

Inputs you’re given

A product catalog: code -> {name, unitPrice}

A list of discount rules (see below)

A cart consisting of line items: {code, quantity}

Output

The cart’s final total and/or an itemized breakdown (subtotal, applied discounts, total).

Discount rules (typical interview set)

Implement discount calculation with at least a few of these rule types:

Percentage off a SKU

Example: 10% off "JALAPENO"

Discount applies to the line total for that SKU.

Fixed amount off the cart with threshold

Example: $15 off orders >= $100

Buy X get Y (BOGO / bundle)

Example: “Buy 2 HABANERO get 1 free”

Usually “free” means discount equals cheapest eligible item’s unit price (or same SKU’s price).

Tiered discounts

Example: 5% off >= $50, 10% off >= $100

Coupon codes

Example: user enters "WELCOME10" to activate a rule set.

Key constraints interviewers usually add
Money correctness

Use integer cents (avoid floating point).

Define rounding rules for percentage discounts (e.g., round half up to the nearest cent).

Stacking / precedence rules (this is the “real” difficulty)

They’ll ask you to clarify and implement one of these policies:

Stackable: apply multiple discounts in sequence (item-level first, then cart-level).

Non-stackable: choose best single discount.

Mixed: item-level discounts stack, but only one cart-level coupon applies.

Limits: max discount per order, or “cannot discount below $0”.

Efficiency

addItem/removeItem should be ~O(1) average.

Total computation should be O(#items + #discountRules).

Example (good for interviews)

Catalog:

A = $30.00

B = $20.00

Cart:

add A x 3, B x 2 → subtotal = $130.00

Discount rules:

A: 10% off

$15 off orders >= $100

BOGO: buy 2 B get 1 free (won’t apply here since B x 2)

If stacking policy is “item-level then cart-level”:

A discount: 10% of $90.00 = $9.00

new subtotal = $121.00

threshold coupon: -$15.00

total = $106.00

---

## Design Diagram (BigDecimal for money)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       ShoppingCartSolution                              │
│                                                                         │
│  ┌────────────────────┐    ┌───────────────────────────┐                │
│  │    Product          │    │     LineItem               │               │
│  │────────────────────│    │───────────────────────────│                │
│  │ - code : String     │    │ - product : Product        │               │
│  │ - name : String     │    │ - quantity : int           │               │
│  │ - unitPrice :       │    │───────────────────────────│                │
│  │     BigDecimal      │    │ + getLineTotal():BigDecimal│               │
│  └────────────────────┘    └───────────────────────────┘                │
│           ▲                         ▲                                   │
│           │has-a                    │has-many                           │
│  ┌────────┴───────────┐    ┌───────┴───────────────────────────┐       │
│  │  ProductCatalog     │    │         Cart                      │       │
│  │────────────────────│    │───────────────────────────────────│       │
│  │ - Map<String,       │    │ - Map<String, LineItem> items     │       │
│  │     Product>        │    │ - catalog : ProductCatalog        │       │
│  │────────────────────│    │───────────────────────────────────│       │
│  │ + addProduct(...)   │    │ + addItem(code, qty)              │       │
│  │ + getProduct(code)  │    │ + removeItem(code, qty)           │       │
│  └────────────────────┘    │ + getItems() : Collection         │       │
│                             │ + subtotal() : BigDecimal         │       │
│                             └──────────────────────────────────┘       │
│                                                                         │
│  ┌──────────────────────────────────────────────────────┐              │
│  │  «interface» DiscountRule                             │              │
│  │──────────────────────────────────────────────────────│              │
│  │ + apply(Cart) : BigDecimal  (discount amount)        │              │
│  └──────────────┬───────────────────────────────────────┘              │
│                 │ implements                                            │
│   ┌─────────────┼──────────────────────────────────────────────┐       │
│   │  ┌──────────┴──────────────┐  ┌──────────────────────────┐ │       │
│   │  │ PercentageOffSku        │  │ FixedAmountOffThreshold  │ │       │
│   │  │ - sku : String          │  │ - threshold : BigDecimal │ │       │
│   │  │ - percent : BigDecimal  │  │ - discount : BigDecimal  │ │       │
│   │  └─────────────────────────┘  └──────────────────────────┘ │       │
│   │  ┌──────────────────────────┐  ┌─────────────────────────┐ │       │
│   │  │ BuyXGetYFree             │  │ TieredDiscount          │ │       │
│   │  │ - sku : String           │  │ - tiers: List<Tier>     │ │       │
│   │  │ - buyQty : int           │  │  (threshold:BigDecimal, │ │       │
│   │  │ - freeQty : int          │  │   percent:BigDecimal)   │ │       │
│   │  └──────────────────────────┘  └─────────────────────────┘ │       │
│   │  ┌──────────────────────────┐                              │       │
│   │  │ CouponCodeRule           │                              │       │
│   │  │ - code : String          │                              │       │
│   │  │ - innerRule : Discount   │                              │       │
│   │  └──────────────────────────┘                              │       │
│   └────────────────────────────────────────────────────────────┘       │
│                                                                         │
│  ┌──────────────────────────────────────────────────────┐              │
│  │  DiscountEngine                                       │              │
│  │──────────────────────────────────────────────────────│              │
│  │ - itemRules : List<DiscountRule>                      │              │
│  │ - cartRules : List<DiscountRule>                      │              │
│  │ - activeCoupons : Set<String>                         │              │
│  │──────────────────────────────────────────────────────│              │
│  │ + addItemRule(DiscountRule)                            │              │
│  │ + addCartRule(DiscountRule)                            │              │
│  │ + applyCoupon(String code)                            │              │
│  │ + totalDiscount(Cart) : BigDecimal                    │              │
│  │   (item-level first, then cart-level)                 │              │
│  └──────────────────────────────────────────────────────┘              │
│                                                                         │
│  ┌──────────────────────────────────────────────────────┐              │
│  │  Checkout (top-level orchestrator)                    │              │
│  │──────────────────────────────────────────────────────│              │
│  │ - cart : Cart                                         │              │
│  │ - discountEngine : DiscountEngine                     │              │
│  │──────────────────────────────────────────────────────│              │
│  │ + subtotal() : BigDecimal                             │              │
│  │ + totalDiscount() : BigDecimal                        │              │
│  │ + total() : BigDecimal                                │              │
│  └──────────────────────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────────────────┘
```

### Design notes
- All monetary values use `BigDecimal` with scale 2, `RoundingMode.HALF_UP`
- Stacking policy: item-level discounts first, then cart-level on reduced subtotal
- `total()` clamped to `>= 0` via `BigDecimal.max(BigDecimal.ZERO)`
- `CouponCodeRule` wraps any `DiscountRule`, only active when coupon is applied