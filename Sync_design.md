Based on the Edge Sync design doc, here are the key trade-offs:

1. Server-as-Source-of-Truth vs. Peer-to-Peer (P1)
Chose	Over	Consequence
Centralized server arbitration	CRDTs / peer-to-peer sync	Pro: Simpler correctness reasoning, single canonical state, easier debugging. Con: Server is a single point of failure — if server is down, clients can accumulate local changes but can't sync. Also adds latency (every write goes through server).
2. Optimistic Concurrency (OCC) vs. Pessimistic Locking (P4)
Chose	Over	Consequence
OCC (baseVersion check, reject on mismatch)	Lock-before-write	Pro: No locks at 500K write QPS — fast happy path. Con: Conflicts require client-side rebase + retry (extra round-trip). If conflict rate is high, clients retry repeatedly. This is why P5 (conflict minimization) exists — the design depends on conflicts being rare.
3. Last-Write-Wins (Server Arbitration) vs. Rich Merge (P5)
Chose	Over	Consequence
Simple deterministic convergence (first accepted write wins)	Complex merge logic (3-way merge, operational transforms)	Pro: Server stays simple, no branching history, deterministic outcome. Con: User intent can be lost — if Device A adds a bookmark and Device B edits a password at the "same time," there's no intelligent merge. Data types that need richer semantics (e.g., collaborative editing) can't use this model.
4. Mutable Data Model vs. Append-Only / Event Sourcing (FR4)
Chose	Over	Consequence
Single mutable copy per entity	Append-only log / event sourcing	Pro: Simpler storage, no unbounded growth, straightforward reads. Con: No built-in history — you can't answer "what was this bookmark 3 days ago?" without separate audit logging. Also harder to debug data loss because there's no historical trail of mutations. The "ghost bookmark" issue the doc mentions is partly a consequence of this — without history, detecting when data was lost is hard.
5. Delta Sync (Preferred) with Snapshot Fallback vs. Always-Full-Sync (FR3)
Chose	Over	Consequence
Delta sync (send only changes since version X)	Always send full dataset	Pro: Orders of magnitude less bandwidth — a user with 5K bookmarks doesn't re-download them all on every sync. Con: Delta correctness is harder — if the version tracking is wrong or a delta is missed, client silently drifts. This is why the snapshot fallback exists as a safety net, and why the consistency health metrics (97.8% → 99.9%) were so important.
6. Conflict Minimization at Source (Client-Side) vs. Server-Side Resolution (P5)
Chose	Over	Consequence
Reduce conflicts via UX (debouncing, coalescing, read-before-write)	Let conflicts happen and resolve them on server	Pro: Fewer conflicts = fewer retries = lower latency. Con: Pushes complexity to the client. Every client platform (Windows, Mac, iOS, Android, Linux) must implement the same debouncing/coalescing logic correctly. A bug in one client's conflict-avoidance code can cause data loss across all devices for that user.
7. Availability (99.95%) vs. Strong Consistency
Chose	Over	Consequence
Eventual consistency across devices	Strongly consistent reads	Pro: Multi-device sync can tolerate temporary divergence — a bookmark added on phone appears on laptop within seconds, not immediately. This allows high availability. Con: Users may see stale data for a brief window. For sensitive data like passwords, this window matters — a user changes a password on one device, the old password still works on another device until sync completes.
The Meta Trade-Off: Simplicity vs. Capability
The entire design philosophy is "make the common case fast and simple, handle the edge cases with safety nets":

Common case: single device writes, no conflict → OCC succeeds first try, delta sync works perfectly
Rare case: multi-device conflict → rebase + retry (extra round-trip, but rare)
Failure case: sync drift → consistency health metrics detect it, snapshot fallback corrects it
Worst case: data loss → deletion telemetry + recovery pipeline restores data
This is a defensible position for an interview: "We optimized for the 99% case (single-device, no conflict) and built observability + recovery for the 1% case, rather than making the common path complex to handle every edge case."