1. Time complexity of get() and write()?
Both are O(1). HashMap.get/put is O(1). DLinkedNode.addFirst/remove/removeLast are all O(1) pointer operations. increaseFreq does a constant number of these O(1) ops. No iteration occurs.

2. Why doubly-linked list per frequency, not a priority queue?
A priority queue (heap) gives O(log n) for insert/remove-min. The doubly-linked list per frequency bucket gives O(1) for all operations. The minFreq pointer eliminates the need to search for the minimum. The trade-off: slightly more memory (one list per frequency level), but the frequency buckets are created lazily and cleaned up when empty.

3. Why store key in the Node?
During eviction, we call removeLast() which returns a Node. We need node.key to do keyMap.remove(evicted.key). Without the key stored in the node, we'd need a reverse map or an O(n) scan.

4. Downside of synchronized?
synchronized locks the entire object — all threads are serialized, even if they access different keys. Under high concurrency, this becomes a bottleneck. Alternatives:

Lock striping: partition keys into N segments, each with its own lock
ReentrantLock: supports tryLock() with timeout, interruptibility
5. Would ReadWriteLock help?
No. get() modifies state (increaseFreq updates freqMap, minFreq, node pointers). It's not a read-only operation. Every call needs the write lock, so ReadWriteLock degenerates to a plain mutex with extra overhead.

6. Optimizing for read-heavy workloads?
Options:

Approximate LFU: batch frequency updates (e.g., TinyLFU in Caffeine). Accept slight staleness in frequency counts to allow truly concurrent reads.
Sharding: partition the cache by key hash into N independent LFU caches, each with its own lock. Reads on different shards don't contend.
Copy-on-write for hot path: keep a volatile snapshot for reads; periodically merge frequency updates.
7. Could you use lock striping for LFU?
It's difficult because minFreq and freqMap are global shared state. When thread A evicts from freqMap.get(minFreq), thread B might be modifying that same list. You'd need:

Per-shard minFreq (requires independent eviction per shard)
Or a global lock just for eviction, with fine-grained locks elsewhere
Sharding into independent sub-caches (each with its own minFreq, freqMap, keyMap) is the cleanest approach.

8. Null keys?
Currently keyMap.get(null) works in HashMap (returns null), so get(null) returns null. But write(null, value) would insert a null key — likely a bug. Fix:

9. Integer overflow on freq?
After ~2.1 billion accesses to the same key, freq overflows to negative, breaking freqMap lookups and minFreq logic. Fixes:

Use long instead of int (practical, ~9.2 × 10^18 limit)
Cap at Integer.MAX_VALUE (stops incrementing, treats all high-freq nodes equally)
Periodic frequency decay: halve all frequencies periodically (also makes the cache more adaptive to recency — this is what Window-TinyLFU does)
10. Adding TTL per entry?
Add long expiryTime to Node. Changes:

On get(): check System.currentTimeMillis() > node.expiryTime → treat as miss, remove node
On write(): set expiryTime = now + ttl
Lazy eviction: only check on access (simple, no background thread)
Active eviction: add a ScheduledExecutorService or a DelayQueue to proactively remove expired entries
11. Adding delete(K key)?
The tricky part: if the deleted node was in the minFreq bucket and it's now empty, minFreq might be stale. But minFreq is only used during eviction in write(), which resets minFreq = 1 after insertion anyway. So it's safe.

12. Distributed LFU cache?
Challenges:

Frequency counts are local — each server sees different access patterns. Options: gossip protocol to share counts, or central coordinator.
Consistent hashing to route keys to specific nodes (each node runs local LFU)
Eviction consistency: if node A evicts key X, node B might still think X exists → use invalidation messages
Real-world systems (e.g., Redis) typically use approximate LFU with local frequency counters and probabilistic decay.
13. O(1) LFU with a single linked list?
Group nodes by frequency in contiguous segments. Maintain a Map<Integer, Node> pointing to each segment's head. To increase a node's frequency: remove it from its segment, insert it at the head of the next segment. If the segment is now empty, merge/remove it. This is the approach described in the paper "An O(1) algorithm for implementing the LFU cache eviction scheme" (Shah et al., 2010).

14. Adding metrics?
Add fields:

Increment in get() (hit/miss) and evict(). Hit rate = hitCount / (hitCount + missCount). Use AtomicLong so metrics can be read without holding the lock.

15. Runtime resize(int newCapacity)?
If shrinking, evict until size fits. If growing, nothing changes — new entries will fill naturally. Must be synchronized to prevent concurrent writes from racing with the eviction loop.