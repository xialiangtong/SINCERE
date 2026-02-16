1. What's the time and space complexity?
Time: O(1) for both get() and write(). HashMap gives O(1) lookup; doubly-linked list gives O(1) insert/remove.

Space: O(capacity). The map and linked list each store at most capacity entries. Each node has constant overhead (key, value, two pointers).

2. Why a doubly-linked list instead of singly-linked?
Singly-linked list requires O(n) to remove a node from the middle — you need the predecessor, which means traversal. Doubly-linked list gives O(1) removal because each node has a prev pointer.

3. Why dummy head/tail sentinels?
Without sentinels, every insert/remove needs null checks for boundary conditions (e.g., "is this the first node?", "is this the last node?"). Sentinels guarantee head.next and tail.prev always exist, eliminating all special cases. Same logic, fewer bugs.

4. Could you use LinkedHashMap instead?
Yes. Java's LinkedHashMap with accessOrder=true maintains insertion/access order. Override removeEldestEntry():

Trade-off: simpler code, but less control over eviction behavior and harder to extend (e.g., adding TTL).

5. How does your synchronized approach compare to ConcurrentHashMap?
ConcurrentHashMap alone doesn't work because get() modifies the linked list (moves node to head) — it's not a read-only operation. You'd still need synchronization around the list operations. synchronized on the whole method is correct and simple, though it serializes all access.

6. How would you improve concurrency?
Options:

Lock striping: partition keys by hash into N segments, each with its own lock + linked list. Reduces contention but complicates the eviction ordering across segments.
Read-write lock with deferred updates: serve reads from the map without locking, batch frequency/order updates asynchronously. This is what Caffeine does.
Lock-free approach: use ConcurrentHashMap for lookups + a concurrent queue for access order. Drain the queue periodically to update the list.
7. How would you add TTL (time-to-live)?
Add long expiryTime to Node. Two strategies:

Lazy expiration: on get(), check if System.currentTimeMillis() > node.expiryTime. If expired, remove and return null. Simple but expired keys consume space until accessed.
Active expiration: use a ScheduledExecutorService or a DelayQueue<Node> to proactively remove expired entries. More complex but frees memory sooner.
8. How would you add a delete(K key) method?
O(1) — remove from map and unlink from list. No eviction logic needed.

9. What if capacity needs to change at runtime?
Evicts LRU entries until size fits. Must be synchronized.

10. LRU vs. LFU — when to use which?
Scenario	Better Policy
Temporal locality (recent items accessed again soon)	LRU
Frequency matters (some items consistently popular)	LFU
Scan/burst resistance (one-time sequential scan shouldn't pollute cache)	LFU
Simplicity	LRU
Adapting to changing access patterns	LRU (LFU is slow to forget old hot items unless decay is added)
Production systems often use hybrid approaches: Caffeine's Window-TinyLFU combines a small LRU "window" with an LFU main cache and an admission filter.

11. How does this compare to OS page replacement?
OS virtual memory uses similar algorithms:

LRU: expensive in hardware (requires tracking access order). Used approximated via clock algorithm (second-chance).
FIFO: simplest, worst performance (Bélády's anomaly).
Optimal (Bélády's): evict the page used farthest in the future. Impossible in practice but useful as a benchmark.
Linux uses a two-list strategy: active list (frequently accessed) + inactive list (candidates for eviction), similar to a simplified LFU/LRU hybrid.
12. How would you make this a distributed LRU cache?
Same approach as distributed LFU:

Consistent hashing routes each key to a specific node → local LRU is sufficient
No cross-node access-order coordination needed — all accesses to key "X" hit the same node
Invalidation: if data changes, broadcast invalidation messages to other nodes' local caches
Real-world: Redis with allkeys-lru policy, Memcached with LRU per slab class
13. What happens if two threads call write() and get() simultaneously without synchronized?
Race conditions:

Thread A calls write("x", 1) — starts addAfterHead
Thread B calls get("y") — starts moveToHead
Both modify head.next concurrently → corrupted linked list (orphaned nodes, circular references, NPE)
map.size() might return stale value → eviction may not trigger or may evict incorrectly