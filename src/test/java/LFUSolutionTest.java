import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LFUSolution Tests")
public class LFUSolutionTest {

    // ==================== Node Tests ====================

    @Nested
    @DisplayName("Node Tests")
    class NodeTests {

        @Test
        @DisplayName("node stores key and value")
        void testNodeKeyValue() {
            LFUSolution.Node<String, Integer> node = new LFUSolution.Node<>("a", 1);
            assertEquals("a", node.key);
            assertEquals(1, node.value);
        }

        @Test
        @DisplayName("node initial freq is 1")
        void testNodeInitialFreq() {
            LFUSolution.Node<String, Integer> node = new LFUSolution.Node<>("a", 1);
            assertEquals(1, node.freq);
        }

        @Test
        @DisplayName("node prev and next are null initially")
        void testNodePrevNextNull() {
            LFUSolution.Node<String, Integer> node = new LFUSolution.Node<>("a", 1);
            assertNull(node.prev);
            assertNull(node.next);
        }
    }

    // ==================== DLinkedNode Tests ====================

    @Nested
    @DisplayName("DLinkedNode Tests")
    class DLinkedNodeTests {

        @Test
        @DisplayName("new DLinkedNode is empty")
        void testNewListEmpty() {
            LFUSolution.DLinkedNode<String, Integer> list = new LFUSolution.DLinkedNode<>();
            assertTrue(list.isEmpty());
            assertEquals(0, list.size);
        }

        @Test
        @DisplayName("addFirst adds node and increments size")
        void testAddFirst() {
            LFUSolution.DLinkedNode<String, Integer> list = new LFUSolution.DLinkedNode<>();
            LFUSolution.Node<String, Integer> node = new LFUSolution.Node<>("a", 1);
            list.addFirst(node);
            assertFalse(list.isEmpty());
            assertEquals(1, list.size);
        }

        @Test
        @DisplayName("addFirst places node right after head")
        void testAddFirstOrder() {
            LFUSolution.DLinkedNode<String, Integer> list = new LFUSolution.DLinkedNode<>();
            LFUSolution.Node<String, Integer> n1 = new LFUSolution.Node<>("a", 1);
            LFUSolution.Node<String, Integer> n2 = new LFUSolution.Node<>("b", 2);
            list.addFirst(n1);
            list.addFirst(n2);
            // n2 should be right after head, n1 before tail
            assertEquals(n2, list.head.next);
            assertEquals(n1, list.tail.prev);
        }

        @Test
        @DisplayName("remove removes node and decrements size")
        void testRemove() {
            LFUSolution.DLinkedNode<String, Integer> list = new LFUSolution.DLinkedNode<>();
            LFUSolution.Node<String, Integer> node = new LFUSolution.Node<>("a", 1);
            list.addFirst(node);
            list.remove(node);
            assertTrue(list.isEmpty());
            assertEquals(0, list.size);
        }

        @Test
        @DisplayName("remove middle node keeps list intact")
        void testRemoveMiddle() {
            LFUSolution.DLinkedNode<String, Integer> list = new LFUSolution.DLinkedNode<>();
            LFUSolution.Node<String, Integer> n1 = new LFUSolution.Node<>("a", 1);
            LFUSolution.Node<String, Integer> n2 = new LFUSolution.Node<>("b", 2);
            LFUSolution.Node<String, Integer> n3 = new LFUSolution.Node<>("c", 3);
            list.addFirst(n1);
            list.addFirst(n2);
            list.addFirst(n3);
            list.remove(n2);
            assertEquals(2, list.size);
            assertEquals(n3, list.head.next);
            assertEquals(n1, list.tail.prev);
        }

        @Test
        @DisplayName("removeLast removes node closest to tail")
        void testRemoveLast() {
            LFUSolution.DLinkedNode<String, Integer> list = new LFUSolution.DLinkedNode<>();
            LFUSolution.Node<String, Integer> n1 = new LFUSolution.Node<>("a", 1);
            LFUSolution.Node<String, Integer> n2 = new LFUSolution.Node<>("b", 2);
            list.addFirst(n1);
            list.addFirst(n2);
            LFUSolution.Node<String, Integer> removed = list.removeLast();
            assertEquals(n1, removed);
            assertEquals(1, list.size);
        }

        @Test
        @DisplayName("removeLast on empty list returns null")
        void testRemoveLastEmpty() {
            LFUSolution.DLinkedNode<String, Integer> list = new LFUSolution.DLinkedNode<>();
            assertNull(list.removeLast());
        }
    }

    // ==================== Get Tests ====================

    @Nested
    @DisplayName("Get Tests")
    class GetTests {

        @Test
        @DisplayName("get on empty cache returns null (cache miss)")
        void testGetEmpty() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(3);
            assertNull(cache.get("a"));
        }

        @Test
        @DisplayName("get non-existent key returns null (cache miss)")
        void testGetMiss() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(3);
            cache.write("a", 1);
            assertNull(cache.get("b"));
        }

        @Test
        @DisplayName("get existing key returns value (cache hit)")
        void testGetHit() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(3);
            cache.write("a", 1);
            assertEquals(1, cache.get("a"));
        }

        @Test
        @DisplayName("get increases frequency of accessed key")
        void testGetIncreasesFreq() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(3);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.get("a"); // freq of a becomes 2
            cache.write("c", 3);
            // "b" has lowest freq (1) and should be evicted
            cache.write("d", 4); // evicts "b"
            assertNull(cache.get("b"));
            assertEquals(1, cache.get("a"));
        }

        @Test
        @DisplayName("get returns correct value after update")
        void testGetAfterUpdate() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(3);
            cache.write("a", 1);
            cache.write("a", 10);
            assertEquals(10, cache.get("a"));
        }
    }

    // ==================== Write Tests ====================

    @Nested
    @DisplayName("Write Tests")
    class WriteTests {

        @Test
        @DisplayName("write new key within capacity")
        void testWriteNewKey() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(3);
            assertTrue(cache.write("a", 1));
            assertEquals(1, cache.get("a"));
        }

        @Test
        @DisplayName("write multiple keys within capacity")
        void testWriteMultipleKeys() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(3);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.write("c", 3);
            assertEquals(1, cache.get("a"));
            assertEquals(2, cache.get("b"));
            assertEquals(3, cache.get("c"));
        }

        @Test
        @DisplayName("write updates existing key value")
        void testWriteUpdateExisting() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(3);
            cache.write("a", 1);
            cache.write("a", 100);
            assertEquals(100, cache.get("a"));
        }

        @Test
        @DisplayName("write evicts LFU key when at capacity")
        void testWriteEvictsLFU() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(2);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.get("a");     // freq: a=2, b=1
            cache.write("c", 3); // evicts "b" (lowest freq)
            assertNull(cache.get("b"));
            assertEquals(1, cache.get("a"));
            assertEquals(3, cache.get("c"));
        }

        @Test
        @DisplayName("write evicts LRU among same frequency")
        void testWriteEvictsLRUAmongSameFreq() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(2);
            cache.write("a", 1);
            cache.write("b", 2);
            // both have freq=1, "a" is least recently used
            cache.write("c", 3); // evicts "a"
            assertNull(cache.get("a"));
            assertEquals(2, cache.get("b"));
            assertEquals(3, cache.get("c"));
        }

        @Test
        @DisplayName("write updates existing key without eviction")
        void testWriteUpdateDoesNotEvict() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(2);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.write("a", 10); // update, not new entry
            assertEquals(10, cache.get("a"));
            assertEquals(2, cache.get("b")); // b should still be present
        }

        @Test
        @DisplayName("write after eviction keeps correct size")
        void testWriteAfterEviction() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(2);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.write("c", 3); // evicts one
            cache.write("d", 4); // evicts one
            // only 2 entries remain
            int count = 0;
            if (cache.get("a") != null) count++;
            if (cache.get("b") != null) count++;
            if (cache.get("c") != null) count++;
            if (cache.get("d") != null) count++;
            assertEquals(2, count);
        }
    }

    // ==================== Corner Case Tests ====================

    @Nested
    @DisplayName("Corner Case Tests")
    class CornerCaseTests {

        @Test
        @DisplayName("capacity 0 - get always returns null")
        void testCapacityZeroGet() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(0);
            assertNull(cache.get("a"));
        }

        @Test
        @DisplayName("capacity 0 - write returns false")
        void testCapacityZeroWrite() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(0);
            assertFalse(cache.write("a", 1));
            assertNull(cache.get("a"));
        }

        @Test
        @DisplayName("negative capacity - write returns false")
        void testNegativeCapacity() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(-5);
            assertFalse(cache.write("a", 1));
            assertNull(cache.get("a"));
        }

        @Test
        @DisplayName("capacity 1 - single entry cache")
        void testCapacityOne() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(1);
            cache.write("a", 1);
            assertEquals(1, cache.get("a"));
            cache.write("b", 2); // evicts "a"
            assertNull(cache.get("a"));
            assertEquals(2, cache.get("b"));
        }

        @Test
        @DisplayName("capacity 1 - update does not evict")
        void testCapacityOneUpdate() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(1);
            cache.write("a", 1);
            cache.write("a", 2); // update, not eviction
            assertEquals(2, cache.get("a"));
        }

        @Test
        @DisplayName("multiple gets then write evicts correct key")
        void testMultipleGetsThenEvict() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(3);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.write("c", 3);
            cache.get("a"); // freq: a=2
            cache.get("b"); // freq: b=2
            cache.get("a"); // freq: a=3
            // freq: a=3, b=2, c=1
            cache.write("d", 4); // evicts "c" (lowest freq)
            assertNull(cache.get("c"));
            assertEquals(1, cache.get("a"));
            assertEquals(2, cache.get("b"));
            assertEquals(4, cache.get("d"));
        }

        @Test
        @DisplayName("write null value is allowed")
        void testWriteNullValue() {
            LFUSolution<String, Integer> cache = new LFUSolution<>(2);
            cache.write("a", null);
            assertNull(cache.get("a")); // value is null but key exists
            // verify key exists by writing again (update path)
            cache.write("a", 42);
            assertEquals(42, cache.get("a"));
        }
    }

    // ==================== Concurrency Tests ====================

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("concurrent writes do not exceed capacity")
        void testConcurrentWritesCapacity() throws Exception {
            int capacity = 100;
            LFUSolution<Integer, Integer> cache = new LFUSolution<>(capacity);
            int threadCount = 10;
            int writesPerThread = 200;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(1);

            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                int offset = t * writesPerThread;
                futures.add(executor.submit(() -> {
                    try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    for (int i = 0; i < writesPerThread; i++) {
                        cache.write(offset + i, offset + i);
                    }
                }));
            }
            latch.countDown(); // start all threads at once
            for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Count how many keys are accessible
            int found = 0;
            for (int i = 0; i < threadCount * writesPerThread; i++) {
                if (cache.get(i) != null) found++;
            }
            assertEquals(capacity, found);
        }

        @Test
        @DisplayName("concurrent reads and writes do not throw")
        void testConcurrentReadWrite() throws Exception {
            LFUSolution<Integer, Integer> cache = new LFUSolution<>(50);
            int threadCount = 8;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger errors = new AtomicInteger(0);

            List<Future<?>> futures = new ArrayList<>();
            // Writers
            for (int t = 0; t < threadCount / 2; t++) {
                int offset = t * 100;
                futures.add(executor.submit(() -> {
                    try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    try {
                        for (int i = 0; i < 100; i++) {
                            cache.write(offset + i, offset + i);
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }));
            }
            // Readers
            for (int t = 0; t < threadCount / 2; t++) {
                futures.add(executor.submit(() -> {
                    try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    try {
                        for (int i = 0; i < 200; i++) {
                            cache.get(i); // may hit or miss
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }));
            }
            latch.countDown();
            for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(0, errors.get(), "No exceptions should be thrown during concurrent access");
        }

        @Test
        @DisplayName("concurrent updates to same key are consistent")
        void testConcurrentUpdateSameKey() throws Exception {
            LFUSolution<String, Integer> cache = new LFUSolution<>(10);
            cache.write("key", 0);
            int threadCount = 10;
            int updatesPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(1);

            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                int threadId = t;
                futures.add(executor.submit(() -> {
                    try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    for (int i = 0; i < updatesPerThread; i++) {
                        cache.write("key", threadId * 1000 + i);
                    }
                }));
            }
            latch.countDown();
            for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Key should still exist with some valid value
            Integer value = cache.get("key");
            assertNotNull(value, "Key should still be in cache after concurrent updates");
        }

        @Test
        @DisplayName("concurrent evictions maintain consistency")
        void testConcurrentEvictions() throws Exception {
            int capacity = 10;
            LFUSolution<Integer, Integer> cache = new LFUSolution<>(capacity);
            int threadCount = 5;
            int writesPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger errors = new AtomicInteger(0);

            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                int offset = t * writesPerThread;
                futures.add(executor.submit(() -> {
                    try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    try {
                        for (int i = 0; i < writesPerThread; i++) {
                            cache.write(offset + i, offset + i);
                            cache.get(offset + i); // bump freq
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }));
            }
            latch.countDown();
            for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(0, errors.get(), "No exceptions during concurrent evictions");

            // Exactly capacity keys should remain
            int found = 0;
            for (int i = 0; i < threadCount * writesPerThread; i++) {
                if (cache.get(i) != null) found++;
            }
            assertTrue(found <= capacity, "Should not exceed capacity: found=" + found);
        }

        @Test
        @DisplayName("high contention mixed operations")
        void testHighContentionMixed() throws Exception {
            LFUSolution<Integer, Integer> cache = new LFUSolution<>(20);
            int threadCount = 16;
            int opsPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger errors = new AtomicInteger(0);

            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                int threadId = t;
                futures.add(executor.submit(() -> {
                    try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    try {
                        for (int i = 0; i < opsPerThread; i++) {
                            int key = (threadId * opsPerThread + i) % 50;
                            if (i % 3 == 0) {
                                cache.write(key, i);
                            } else {
                                cache.get(key);
                            }
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }));
            }
            latch.countDown();
            for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(0, errors.get(), "No exceptions during high contention");
        }
    }
}
