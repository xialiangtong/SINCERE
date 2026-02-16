import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LRUSolution Tests")
public class LRUSolutionTest {

    // ==================== Node Tests ====================

    @Nested
    @DisplayName("Node Tests")
    class NodeTests {

        @Test
        @DisplayName("node stores key and value")
        void testNodeKeyValue() {
            LRUSolution.Node<String, Integer> node = new LRUSolution.Node<>("a", 1);
            assertEquals("a", node.key);
            assertEquals(1, node.value);
        }

        @Test
        @DisplayName("node prev and next are null initially")
        void testNodePrevNextNull() {
            LRUSolution.Node<String, Integer> node = new LRUSolution.Node<>("a", 1);
            assertNull(node.prev);
            assertNull(node.next);
        }

        @Test
        @DisplayName("node value can be updated")
        void testNodeValueUpdate() {
            LRUSolution.Node<String, Integer> node = new LRUSolution.Node<>("a", 1);
            node.value = 99;
            assertEquals(99, node.value);
        }

        @Test
        @DisplayName("node with null key and value")
        void testNodeNullKeyValue() {
            LRUSolution.Node<String, Integer> node = new LRUSolution.Node<>(null, null);
            assertNull(node.key);
            assertNull(node.value);
        }

        @Test
        @DisplayName("nodes can be linked together")
        void testNodeLinking() {
            LRUSolution.Node<String, Integer> n1 = new LRUSolution.Node<>("a", 1);
            LRUSolution.Node<String, Integer> n2 = new LRUSolution.Node<>("b", 2);
            n1.next = n2;
            n2.prev = n1;
            assertEquals(n2, n1.next);
            assertEquals(n1, n2.prev);
        }
    }

    // ==================== Get Tests ====================

    @Nested
    @DisplayName("Get Tests")
    class GetTests {

        @Test
        @DisplayName("get on empty cache returns null")
        void testGetEmpty() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(3);
            assertNull(cache.get("a"));
        }

        @Test
        @DisplayName("get non-existent key returns null")
        void testGetMiss() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(3);
            cache.write("a", 1);
            assertNull(cache.get("b"));
        }

        @Test
        @DisplayName("get existing key returns value")
        void testGetHit() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(3);
            cache.write("a", 1);
            assertEquals(1, cache.get("a"));
        }

        @Test
        @DisplayName("get marks key as recently used")
        void testGetMarksRecent() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(2);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.get("a");          // "a" is now MRU, "b" is LRU
            cache.write("c", 3);     // evicts "b"
            assertNull(cache.get("b"));
            assertEquals(1, cache.get("a"));
            assertEquals(3, cache.get("c"));
        }

        @Test
        @DisplayName("get returns updated value")
        void testGetAfterUpdate() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(3);
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
        void testWriteNew() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(3);
            assertTrue(cache.write("a", 1));
            assertEquals(1, cache.get("a"));
        }

        @Test
        @DisplayName("write multiple keys within capacity")
        void testWriteMultiple() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(3);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.write("c", 3);
            assertEquals(1, cache.get("a"));
            assertEquals(2, cache.get("b"));
            assertEquals(3, cache.get("c"));
        }

        @Test
        @DisplayName("write updates existing key")
        void testWriteUpdate() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(3);
            cache.write("a", 1);
            cache.write("a", 100);
            assertEquals(100, cache.get("a"));
        }

        @Test
        @DisplayName("write evicts LRU when at capacity")
        void testWriteEvictsLRU() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(2);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.write("c", 3);  // evicts "a" (LRU)
            assertNull(cache.get("a"));
            assertEquals(2, cache.get("b"));
            assertEquals(3, cache.get("c"));
        }

        @Test
        @DisplayName("write update does not evict")
        void testWriteUpdateNoEvict() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(2);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.write("a", 10);  // update, not new
            assertEquals(10, cache.get("a"));
            assertEquals(2, cache.get("b"));
        }

        @Test
        @DisplayName("write update makes key MRU")
        void testWriteUpdateMakesRecent() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(2);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.write("a", 10);  // "a" becomes MRU
            cache.write("c", 3);   // evicts "b" (LRU)
            assertNull(cache.get("b"));
            assertEquals(10, cache.get("a"));
            assertEquals(3, cache.get("c"));
        }

        @Test
        @DisplayName("sequential evictions")
        void testSequentialEvictions() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(2);
            cache.write("a", 1);
            cache.write("b", 2);
            cache.write("c", 3);  // evicts "a"
            cache.write("d", 4);  // evicts "b"
            assertNull(cache.get("a"));
            assertNull(cache.get("b"));
            assertEquals(3, cache.get("c"));
            assertEquals(4, cache.get("d"));
        }
    }

    // ==================== Corner Case Tests ====================

    @Nested
    @DisplayName("Corner Case Tests")
    class CornerCaseTests {

        @Test
        @DisplayName("capacity 0 - get returns null")
        void testCapZeroGet() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(0);
            assertNull(cache.get("a"));
        }

        @Test
        @DisplayName("capacity 0 - write returns false")
        void testCapZeroWrite() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(0);
            assertFalse(cache.write("a", 1));
            assertNull(cache.get("a"));
        }

        @Test
        @DisplayName("negative capacity - write returns false")
        void testNegativeCap() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(-5);
            assertFalse(cache.write("a", 1));
        }

        @Test
        @DisplayName("capacity 1")
        void testCapOne() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(1);
            cache.write("a", 1);
            assertEquals(1, cache.get("a"));
            cache.write("b", 2);  // evicts "a"
            assertNull(cache.get("a"));
            assertEquals(2, cache.get("b"));
        }

        @Test
        @DisplayName("capacity 1 - update does not evict")
        void testCapOneUpdate() {
            LRUSolution<String, Integer> cache = new LRUSolution<>(1);
            cache.write("a", 1);
            cache.write("a", 2);
            assertEquals(2, cache.get("a"));
        }

        @Test
        @DisplayName("LeetCode example 1")
        void testLeetCodeExample() {
            LRUSolution<Integer, Integer> cache = new LRUSolution<>(2);
            cache.write(1, 1);
            cache.write(2, 2);
            assertEquals(1, cache.get(1));    // returns 1
            cache.write(3, 3);               // evicts key 2
            assertNull(cache.get(2));         // returns -1 (null)
            cache.write(4, 4);               // evicts key 1
            assertNull(cache.get(1));         // returns -1 (null)
            assertEquals(3, cache.get(3));    // returns 3
            assertEquals(4, cache.get(4));    // returns 4
        }
    }
}
