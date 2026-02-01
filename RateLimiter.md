Problem: Build a Rate Limiter (Throttling Utility)

Implement a reusable utility that enforces request limits per key (e.g., per user, IP, API key). The limiter should decide whether a request is allowed right now and optionally report retry-after.

You can implement either:

Fixed window rate limiting (simpler), or

Token bucket (more flexible / smoother)

Requirements
1) Core API

Design an API like:

allow(key: string) -> Decision

Where Decision includes:

allowed: bool

retry_after_ms?: number (if not allowed)

(optional) remaining?: number (if meaningful)

Or a variant:

tryAcquire(key, permits=1) -> bool

2) Configuration

Limiter should be configurable with:

capacity / max_requests

refill_rate (token bucket) OR window_size_ms (fixed window)

optional permits_per_request (default 1)

3) Correctness rules

Limits are enforced independently per key

For each request:

If allowed: update internal state (consume token / increment counter)

If rejected: do not exceed the configured limit

Must behave correctly when time advances (refill/reset logic)

Option A: Fixed Window Limiter (simpler)

Model: count requests in discrete windows of length window_size_ms.

Example config: “max 10 requests per 60 seconds”.

State per key:

window_start_ms

count

Algorithm:

now = clock.now()

If now >= window_start + window_size:

reset: window_start = floor(now/window_size)*window_size (or just set to now)

count = 0

If count < max_requests:

count++, allow

else reject with:

retry_after = (window_start + window_size) - now

Pros: easy
Cons: bursty at window boundaries

Option B: Token Bucket Limiter (smoother)

Model: tokens accumulate over time at a refill rate, up to a capacity.

Example config: capacity 10, refill 10 tokens per 60 seconds (≈0.1667 tokens/sec)

State per key:

tokens (can be fractional)

last_refill_ms

Algorithm:

now = clock.now()

Compute elapsed: delta = now - last_refill_ms

Refill:

tokens = min(capacity, tokens + delta * refill_rate_tokens_per_ms)

last_refill_ms = now

If tokens >= permits:

tokens -= permits, allow

else reject:

compute tokens needed: need = permits - tokens

retry_after_ms = ceil(need / refill_rate_tokens_per_ms)

Pros: smooth limiting, less boundary burst
Cons: slightly more math 