Areas to Improve
Missing tradeoff discussion. You state decisions (e.g., "server is source of truth", "mutable data not append-only") but don't explain what alternatives you considered and why you rejected them. Shopify interviewers value tradeoff reasoning heavily.
No mention of data model details. What does a sync record look like? How is versioning implemented (logical clock? hybrid clock? timestamp?)? What's the partition key?
Redis caching for conflict-tagged data is mentioned briefly — but no depth on cache invalidation, consistency between Redis and the primary store, or failure modes.
No discussion of how this system would translate to Shopify's domain (e-commerce, merchant data, inventory sync across channels). Be prepared to bridge the gap.

Possible Interview Questions & Challenges
Architecture Deep Dives
"Walk me through a write flow end-to-end." — From client intent → server validation → persistence → propagation to other devices. Include failure handling at each step.
"What happens when 3 devices are offline for a week and come back online simultaneously?" — They'll probe your delta sync vs. snapshot fallback logic, thundering herd protection, and convergence guarantees.
"Why server-as-source-of-truth instead of CRDTs or peer-to-peer?" — Be ready to articulate why CRDTs were rejected (complexity? performance? data types don't fit?), not just why server authority was chosen.
"How do you handle the case where the server accepts a write, persists it, but fails to propagate?" — Partial failure modes. What's your at-least-once / exactly-once guarantee?
Conflict Resolution
"You reduced conflicts by 80%. What were the remaining 20%? Are they solvable?" — Shows you understand the limits of your design.
"Last-write-wins is lossy by definition. How do you justify data loss from LWW as acceptable?" — They'll challenge whether your conflict model actually resolves conflicts or just hides them.
"Give me a concrete example of a conflict that has 'no user-observable impact'." — They'll want to see you reason about specific user scenarios, not just abstract principles.
Consistency & Correctness
"How did you measure consistency at 99.9%? What does the 0.1% failure look like?" — Measurement methodology, sampling, false positives/negatives.
"If the server is source of truth, how do you handle server-side corruption or bad writes?" — Single source of truth is a single point of failure for correctness.
"How do you test sync correctness at scale? What's your testing strategy?" — Chaos testing, property-based testing, shadow traffic, etc.
Scale & Performance
"500K write QPS — what's your storage layer? How do you handle hot partitions (power users)?" — Storage technology choice, sharding strategy, rate limiting per user.
"How does the OCC (optimistic concurrency) model perform under high contention?" — A user editing bookmarks rapidly on one device while another device syncs — retry storms, livelock potential.
"What's your read-to-write ratio and how does that influence your architecture?" — 300K read vs 500K write is unusual (write-heavy). Be ready to explain why.
Data Loss Prevention
"Walk me through the ghost bookmark recovery in detail — how did you detect it, what was root cause, and how did you fix it without making it worse?" — This is your strongest story. Have it crisp with timeline, investigation steps, and blast radius.
"How do you distinguish between intentional bulk deletes (user clearing bookmarks) and accidental ones (buggy extension)?" — The heuristic design for deletion telemetry.
Leadership & Decision Making
"What was the hardest technical disagreement on this project and how did you resolve it?"
"If you had to start over, what would you do differently?"
"How did you prioritize what to build first with only 4 engineers?"
Shopify-Specific Bridge Questions
"How would you apply these sync/consistency patterns to Shopify's inventory system across online store, POS, and marketplace channels?" — Shopify cares about multi-channel consistency for merchants. Your sync expertise maps directly.
"Shopify operates at 80M+ RPS during flash sales (e.g., Black Friday). How would your approach to conflict resolution work for inventory decrements vs. bookmark syncs?" — They'll test if you can adapt your thinking to a different domain with harder constraints (inventory can't be "eventually consistent" — overselling is real money).
Preparation Recommendations
Prepare a 2-minute elevator pitch of the system that covers: problem, your approach, key results.
Practice whiteboarding the architecture from scratch — don't rely on the diagram.
Have 3 concrete debugging/investigation stories ready with technical depth (ghost bookmarks is great, prepare 2 more).
Prepare "alternatives considered" for every major design decision — especially server-as-truth vs. CRDTs, mutable vs. append-only, OCC vs. pessimistic locking.
Study Shopify's architecture (shop-mong, Vitess/MySQL, Rails monolith decomposition, flash sale patterns) so you can connect your experience to their problems.