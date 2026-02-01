Problem: Implement Cart Pricing for Checkout

Build a pricing engine that computes an order total from:

cart items

discounts/promotions

shipping rules

taxes (placeholder)

The goal is correctness, clear modeling, and easy extension.

Inputs
Cart items

Each item has:

sku: string

unit_price: decimal (e.g., 19.99)

quantity: int (>= 1)

Example input:

{
  "items": [
    { "sku": "A", "unit_price": "19.99", "quantity": 2 },
    { "sku": "B", "unit_price": "5.00",  "quantity": 1 }
  ],
  "shipping_address": { "country": "US", "state": "WA", "zip": "98056" },
  "discounts": [ ... ]
}

Discounts/promotions

Start with a simple model, then add more types.
Common v1 types:

Percent off: e.g. 10% off entire cart

Fixed amount off: e.g. $5 off cart (not below $0)

SKU-specific percent off: e.g. 20% off SKU “A”

Output

Return a pricing breakdown (not just a number):

{
  "subtotal": "44.98",
  "discount_total": "5.00",
  "shipping": "4.99",
  "tax": "0.00",
  "total": "44.97",
  "line_items": [
    {
      "sku": "A",
      "quantity": 2,
      "unit_price": "19.99",
      "line_subtotal": "39.98",
      "line_discount": "4.00",
      "line_total": "35.98"
    }
  ],
  "applied_discounts": [
    { "type": "PERCENT_OFF_SKU", "sku": "A", "value": "20%", "amount": "4.00" }
  ]
}


Key idea: make it explainable (Shopify cares about transparency and correctness).

Core rules (v1)
1) Subtotal

subtotal = sum(unit_price * quantity) across all items.

2) Discounts

Apply discounts according to a defined order (you must choose and document). A common simple approach:

SKU-specific discounts

Cart-wide discounts

Constraints:

Discounts cannot reduce any line below $0.

Cart total cannot go below $0.

3) Shipping

Implement simple rules:

Base shipping = $7.00

Free shipping if post-discount subtotal >= $50.00

(Optionally) shipping varies by country; otherwise reject non-US.

4) Tax placeholder

For v1, return:

tax = 0.00 (or a stub function that returns 0)

Extension features (what you add after v1)
A) “Buy X Get Y” (BXGY)

Example promo:

Buy 2 of SKU A, get 1 of SKU B free

or Buy 1 get 1 free on the same SKU (BOGO)

Rules you must define:

Does “free” mean free item price is discounted (i.e., discount applied to Y)?

How many times can it apply? (e.g., unlimited or max N)

If multiple eligible Ys exist, which ones are discounted?
Typical: discount the cheapest eligible items first (or exact SKU Y).

Example:

Cart: A x2, B x2 @ $5

Promo: buy 2 A get 1 B free

Discount: $5.00 (one B)

B) Tiered discounts

Two common forms:

(1) Spend tiers

If pre-tax total ≥ $50 → 10% off

≥ $100 → 15% off

≥ $200 → 20% off

(2) Quantity tiers by SKU

SKU A: buy 1–4 → no discount

5–9 → 10% off A

10+ → 20% off A

Rules to define:

Is tier based on subtotal before discounts or after SKU promos?

If multiple tiers apply, pick the best one (usually max discount).

C) Rounding rules

Money handling is where lots of bugs happen.

Rules to define explicitly:

Use decimal, not float.

Round at 2 decimal places.

Decide where rounding happens:

Round each line item total and sum lines (common in invoices), or

Sum precisely then round final totals.

If allocating a cart-wide discount across lines, define allocation:

proportional by line subtotal

handle remainder pennies deterministically (e.g., distribute to highest lines first)

Constraints / edge cases to handle

Invalid quantity (0/negative) → error

Negative prices → error

Unknown SKU in a SKU-specific promo → ignore or error (choose)

Promotions that would exceed available quantity (BXGY) → apply as many times as possible

Conflicting promotions:

Can multiple discounts stack on same line? If yes, in what order?

If no, choose best discount per line/cart.

Suggested clean design (what interviewers like)

Pure calculation core:

calculate(cart, promotions, shippingPolicy, taxPolicy) -> Receipt

Small composable components:

Promotion.apply(cartState) -> adjustments

ShippingCalculator.calculate(cartState) -> shipping

TaxCalculator.calculate(cartState) -> tax (stub)

Avoid mixing parsing/printing with domain logic.

Tests (high-signal set)

Subtotal calculation (multiple SKUs)

Percent-off SKU discount

Fixed-off cart discount with floor at $0

Free shipping threshold hit / not hit

BXGY applies once, applies multiple times, and handles insufficient quantity

Tiered spend discount selects correct tier

Rounding: allocation and deterministic penny handling (at least one test)