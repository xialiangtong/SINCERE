import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LFUCache Tests")
public class LFUCacheTest {

    // ==================== LeetCode Example ====================

    @Nested
    @DisplayName("LeetCode Example Tests")
    class LeetCodeExampleTests {

        @Test
        @DisplayName("Example 1 from problem description")
        void testExample1() {
            LFUCache lfu = new LFUCache(2);
            lfu.put(1, 1);   // cache=[1,_], cnt(1)=1
            lfu.put(2, 2);   // cache=[2,1], cnt(2)=1, cnt(1)=1
            assertEquals(1, lfu.get(1));  // cnt(1)=2, cache=[1,2]
            lfu.put(3, 3);   // evict key 2 (LFU cnt=1), cache=[3,1]
            assertEquals(-1, lfu.get(2)); // not found
            assertEquals(3, lfu.get(3));  // cnt(3)=2, cache=[3,1]
            lfu.put(4, 4);   // evict key 1 (cnt=2 tie, but 1 is LRU), cache=[4,3]
            assertEquals(-1, lfu.get(1)); // not found
            assertEquals(3, lfu.get(3));  // cnt(3)=3
            assertEquals(4, lfu.get(4));  // cnt(4)=2
        }
    }

    // ==================== Basic Operations ====================

    @Nested
    @DisplayName("Basic Operations Tests")
    class BasicOperationsTests {

        @Test
        @DisplayName("get from empty cache returns -1")
        void testGetEmpty() {
            LFUCache cache = new LFUCache(3);
            assertEquals(-1, cache.get(1));
        }

        @Test
        @DisplayName("put and get single element")
        void testPutGetSingle() {
            LFUCache cache = new LFUCache(1);
            cache.put(1, 10);
            assertEquals(10, cache.get(1));
        }

        @Test
        @DisplayName("put updates existing key value")
        void testPutUpdatesValue() {
            LFUCache cache = new LFUCache(2);
            cache.put(1, 10);
            cache.put(1, 20);
            assertEquals(20, cache.get(1));
        }

        @Test
        @DisplayName("get non-existent key returns -1")
        void testGetNonExistent() {
            LFUCache cache = new LFUCache(2);
            cache.put(1, 10);
            assertEquals(-1, cache.get(99));
        }
    }

    // ==================== Eviction Tests ====================

    @Nested
    @DisplayName("Eviction Tests")
    class EvictionTests {

        @Test
        @DisplayName("evicts least frequently used key")
        void testEvictsLFU() {
            LFUCache cache = new LFUCache(2);
            cache.put(1, 1);
            cache.put(2, 2);
            cache.get(1);     // freq(1)=2, freq(2)=1
            cache.put(3, 3);  // evict key 2 (lowest freq)
            assertEquals(-1, cache.get(2));
            assertEquals(1, cache.get(1));
            assertEquals(3, cache.get(3));
        }

        @Test
        @DisplayName("evicts LRU when frequencies are tied")
        void testEvictsLRUOnTie() {
            LFUCache cache = new LFUCache(2);
            cache.put(1, 1);  // freq(1)=1
            cache.put(2, 2);  // freq(2)=1, both freq=1, key 1 is LRU
            cache.put(3, 3);  // evict key 1
            assertEquals(-1, cache.get(1));
            assertEquals(2, cache.get(2));
            assertEquals(3, cache.get(3));
        }

        @Test
        @DisplayName("eviction with capacity 1")
        void testEvictionCapacity1() {
            LFUCache cache = new LFUCache(1);
            cache.put(1, 1);
            cache.put(2, 2); // evict key 1
            assertEquals(-1, cache.get(1));
            assertEquals(2, cache.get(2));
        }

        @Test
        @DisplayName("repeated evictions")
        void testRepeatedEvictions() {
            LFUCache cache = new LFUCache(2);
            cache.put(1, 1);
            cache.put(2, 2);
            cache.put(3, 3); // evict 1 (LRU among freq=1)
            cache.put(4, 4); // evict 2 (LRU among freq=1, since 3 was just inserted)
            // Wait — 3 has freq=1, 2 was evicted already. Let's check:
            assertEquals(-1, cache.get(1));
            assertEquals(-1, cache.get(2));
            assertEquals(3, cache.get(3));
            assertEquals(4, cache.get(4));
        }
    }

    // ==================== Frequency Tracking Tests ====================

    @Nested
    @DisplayName("Frequency Tracking Tests")
    class FrequencyTrackingTests {

        @Test
        @DisplayName("get increments frequency")
        void testGetIncrementsFreq() {
            LFUCache cache = new LFUCache(3);
            cache.put(1, 1); // freq=1
            cache.put(2, 2); // freq=1
            cache.put(3, 3); // freq=1
            cache.get(1);    // freq(1)=2
            cache.get(1);    // freq(1)=3
            cache.get(2);    // freq(2)=2
            // Evict: key 3 has lowest freq (1)
            cache.put(4, 4);
            assertEquals(-1, cache.get(3));
            assertEquals(1, cache.get(1));
            assertEquals(2, cache.get(2));
            assertEquals(4, cache.get(4));
        }

        @Test
        @DisplayName("put on existing key increments frequency")
        void testPutExistingIncrementsFreq() {
            LFUCache cache = new LFUCache(2);
            cache.put(1, 1);  // freq(1)=1
            cache.put(2, 2);  // freq(2)=1
            cache.put(1, 10); // update, freq(1)=2
            cache.put(3, 3);  // evict key 2 (freq=1 < freq(1)=2)
            assertEquals(10, cache.get(1));
            assertEquals(-1, cache.get(2));
            assertEquals(3, cache.get(3));
        }

        @Test
        @DisplayName("minFreq resets to 1 on new insertion")
        void testMinFreqResetsOnInsert() {
            LFUCache cache = new LFUCache(2);
            cache.put(1, 1);
            cache.get(1);    // freq(1)=2
            cache.get(1);    // freq(1)=3
            cache.put(2, 2); // freq(2)=1, minFreq should be 1
            cache.put(3, 3); // evict key 2 (freq=1, the new min)
            assertEquals(1, cache.get(1));
            assertEquals(-1, cache.get(2));
            assertEquals(3, cache.get(3));
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("capacity 0 never stores anything")
        void testCapacityZero() {
            LFUCache cache = new LFUCache(0);
            cache.put(1, 1);
            assertEquals(-1, cache.get(1));
        }

        @Test
        @DisplayName("large capacity no eviction")
        void testLargeCapacityNoEviction() {
            LFUCache cache = new LFUCache(100);
            for (int i = 0; i < 100; i++) {
                cache.put(i, i * 10);
            }
            for (int i = 0; i < 100; i++) {
                assertEquals(i * 10, cache.get(i));
            }
        }

        @Test
        @DisplayName("put same key many times does not grow size")
        void testPutSameKeyDoesNotGrow() {
            LFUCache cache = new LFUCache(1);
            cache.put(1, 1);
            cache.put(1, 2);
            cache.put(1, 3);
            assertEquals(3, cache.get(1));
            // Still room for the one key, no eviction needed
        }

        @Test
        @DisplayName("evict then re-insert same key")
        void testEvictThenReinsert() {
            LFUCache cache = new LFUCache(1);
            cache.put(1, 1);
            cache.put(2, 2); // evict 1
            assertEquals(-1, cache.get(1));
            cache.put(1, 10); // evict 2, re-insert 1
            assertEquals(10, cache.get(1));
            assertEquals(-1, cache.get(2));
        }

        @Test
        @DisplayName("complex sequence with multiple frequency levels")
        void testComplexSequence() {
            LFUCache cache = new LFUCache(3);
            cache.put(1, 1); // freq=1
            cache.put(2, 2); // freq=1
            cache.put(3, 3); // freq=1
            cache.get(1);    // freq(1)=2
            cache.get(2);    // freq(2)=2
            cache.get(3);    // freq(3)=2
            cache.get(1);    // freq(1)=3
            // All have freq>=2, key 2 is LRU among freq=2
            cache.put(4, 4); // evict 2 (freq=2, LRU among 2,3)
            assertEquals(1, cache.get(1));
            assertEquals(-1, cache.get(2));
            assertEquals(3, cache.get(3));
            assertEquals(4, cache.get(4));
        }

        @Test
        @DisplayName("value 0 is valid")
        void testValueZero() {
            LFUCache cache = new LFUCache(2);
            cache.put(1, 0);
            assertEquals(0, cache.get(1));
        }
    }
}
