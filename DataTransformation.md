Build a small program/library that ingests orders from either CSV or JSON, normalizes them into a canonical internal model, aggregates a few metrics, and outputs results in a requested format.

You should prioritize:

Clear types / data model

Small pure functions

Robust error handling

Tests

Input
Input A: CSV orders

File has a header row. Example:

order_id,created_at,customer_id,currency,items,subtotal,discount,tax,shipping
1001,2026-01-12T10:03:22Z,C001,USD,"sku=A12;qty=2;price=19.99|sku=B55;qty=1;price=5.00",44.98,5.00,3.60,4.99
1002,2026-01-12T11:15:00Z,C002,USD,"sku=A12;qty=1;price=19.99",19.99,0.00,1.60,0.00


CSV notes:

items is a string encoding multiple items separated by |

Each item has sku, qty, price separated by ;

Money fields are decimal strings

Input B: JSON orders

Example:

[
  {
    "id": "1003",
    "createdAt": "2026-01-12T12:05:00Z",
    "customer": { "id": "C001" },
    "currency": "USD",
    "lineItems": [
      { "sku": "B55", "quantity": 3, "unitPrice": 5.0 }
    ],
    "charges": { "subtotal": 15.0, "discount": 0, "tax": 1.2, "shipping": 4.99 }
  }
]


JSON notes:

field names differ (id vs order_id, lineItems vs encoded items)

some numeric fields may be numbers (not strings)

Normalization step (canonical model)

Define a canonical internal representation (example):

Order

order_id: string

created_at: Instant (or ISO string normalized)

customer_id: string

currency: string (e.g. USD)

items: List[Item]

charges: Charges

Item

sku: string

qty: int (must be > 0)

unit_price: Decimal (must be ≥ 0)

Charges

subtotal: Decimal (≥ 0)

discount: Decimal (≥ 0)

tax: Decimal (≥ 0)

shipping: Decimal (≥ 0)

Normalization rules:

Trim whitespace on IDs/SKUs

Coerce numeric fields to Decimal safely (no float rounding in internal model if you can avoid it)

Validate:

required fields exist

qty is positive integer

currency is present and consistent per order

subtotal should equal sum(qty * unit_price) within a tolerance or else mark as error (you decide strict vs lenient)

Produce either:

a list of valid normalized orders, and

a list of per-row/per-order errors (don’t just crash)

Aggregation metrics (compute from normalized orders)

Compute these metrics:

1) Total revenue (per currency)

Define:
order_total = subtotal - discount + tax + shipping

Output:

total_revenue_by_currency: Map[currency -> Decimal]

2) Top N products by gross sales

Gross sales per SKU:
sku_gross = sum(qty * unit_price) across all orders (ignore discount allocation unless you want a bonus)

Output:

top_products: List[{ sku, gross_sales, units_sold }] sorted by:

gross_sales desc

tie-breaker sku asc

3) Returning customers count

Customer is “returning” if they have 2+ orders.

Output:

returning_customers: int

4) Daily order count (UTC date)

Output:

orders_by_date: Map[YYYY-MM-DD -> int]

Output

Support one output format (good enough) and optionally a second.

Option 1: JSON report (recommended)

Example:

{
  "total_revenue_by_currency": { "USD": "73.36" },
  "top_products": [
    { "sku": "A12", "gross_sales": "59.97", "units_sold": 3 },
    { "sku": "B55", "gross_sales": "20.00", "units_sold": 4 }
  ],
  "returning_customers": 1,
  "orders_by_date": { "2026-01-12": 3 },
  "errors": [
    { "source": "csv", "row": 5, "message": "qty must be > 0" }
  ]
}

Option 2: CSV outputs (bonus)

top_products.csv

orders_by_date.csv

errors.csv

┌─────────────────────────────────────────────────────────────────────────────┐
│                          DATA TRANSFORMATION PIPELINE                        │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────┐     ┌─────────────┐
                    │  CSV File   │     │  JSON File  │
                    └──────┬──────┘     └──────┬──────┘
                           │                   │
                           ▼                   ▼
                    ┌─────────────┐     ┌─────────────┐
                    │  CsvParser  │     │ JsonParser  │
                    └──────┬──────┘     └──────┬──────┘
                           │                   │
                           └─────────┬─────────┘
                                     │
                                     ▼
                    ┌─────────────────────────────────┐
                    │    <<interface>> OrderParser    │
                    │  + parse(input): ParseResult    │
                    └─────────────────────────────────┘
                                     │
                                     ▼
                    ┌─────────────────────────────────┐
                    │          ParseResult            │
                    │  - orders: List<Order>          │
                    │  - errors: List<ParseError>     │
                    └────────────────┬────────────────┘
                                     │
                                     ▼
                    ┌─────────────────────────────────┐
                    │       OrderAggregator           │
                    │  + aggregate(orders): Report    │
                    └────────────────┬────────────────┘
                                     │
                                     ▼
                    ┌─────────────────────────────────┐
                    │            Report               │
                    │  - revenueByCountry             │
                    │  - topProducts                  │
                    │  - returningCustomers           │
                    │  - ordersByDate                 │
                    │  - errors                       │
                    └────────────────┬────────────────┘
                                     │
                                     ▼
                    ┌─────────────────────────────────┐
                    │   <<interface>> ReportWriter    │
                    │  + write(report): String        │
                    └─────────────────────────────────┘
                           │                   │
                           ▼                   ▼
                    ┌─────────────┐     ┌─────────────┐
                    │ JsonWriter  │     │ CsvWriter   │
                    └─────────────┘     └─────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                            CANONICAL MODEL                                   │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────┐
│           Order             │
├─────────────────────────────┤
│ - orderId: String           │
│ - createdAt: Instant        │
│ - customerId: String        │
│ - currency: String          │
│ - items: List<Item>         │
│ - charges: Charges          │
├─────────────────────────────┤
│ + getTotal(): BigDecimal    │
└─────────────────────────────┘
              │
              │ contains
              ▼
┌─────────────────────────────┐     ┌─────────────────────────────┐
│           Item              │     │         Charges             │
├─────────────────────────────┤     ├─────────────────────────────┤
│ - sku: String               │     │ - subtotal: BigDecimal      │
│ - quantity: int             │     │ - discount: BigDecimal      │
│ - unitPrice: BigDecimal     │     │ - tax: BigDecimal           │
├─────────────────────────────┤     │ - shipping: BigDecimal      │
│ + getLineTotal(): BigDecimal│     └─────────────────────────────┘
└─────────────────────────────┘


┌─────────────────────────────┐     ┌─────────────────────────────┐
│        ParseError           │     │      ProductStats           │
├─────────────────────────────┤     ├─────────────────────────────┤
│ - source: String (csv/json) │     │ - sku: String               │
│ - row: int                  │     │ - grossSales: BigDecimal    │
│ - message: String           │     │ - unitsSold: int            │
└─────────────────────────────┘     └─────────────────────────────┘                    